package com.lion.shopmanager.ui.input

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lion.shopmanager.R
import com.lion.shopmanager.data.AppDatabase
import com.lion.shopmanager.data.Image
import com.lion.shopmanager.data.ImageFileManager
import com.lion.shopmanager.data.ProductInfo
import com.lion.shopmanager.databinding.FragmentInputBinding
import com.lion.shopmanager.ui.extensions.setErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class InputFragment : Fragment(), ImageItemClickListener {
    private var _binding: FragmentInputBinding? = null
    private val binding get() = _binding!!

    private val adapter: InputImageAdapter by lazy { InputImageAdapter(this) }

    private var selectedImagePos: Int = -1
    private val imageList = mutableListOf<Image>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar()
        setRecyclerView()
        onClickSaveBtn()
        maxLengthProductLabel()
        onClickCardView()
        onClickImageView()
    }


    private fun setToolbar() {
        binding.toolbarInput.setNavigationOnClickListener {
            moveToHomeFragment()
        }
    }


    private fun moveToHomeFragment() {
        parentFragmentManager.popBackStack()
    }


    private fun settingDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.CustomMaterialDialogTheme)
        builder.setTitle("저장")
        builder.setMessage("저장하시겠습니까?")
        builder.setCancelable(false)
        builder.setNeutralButton("취소") { _: DialogInterface, _: Int ->
            binding.btnInputSave.isEnabled = true
        }

        builder.setPositiveButton("저장") { _: DialogInterface, _: Int ->
            if (inputProductInfoForDB()) {
                savePhotoToDatabase()
            } else {
                binding.btnInputSave.isEnabled = true
            }
        }
        builder.show()
    }

    private fun maxLengthProductLabel() {
        binding.tfInputProductLabel.editText?.addTextChangedListener { text ->
            if (text != null) {
                binding.tvInputProductLabelCount.text = "( ${text.length} / 200)"
            }
        }
    }

    private fun setRecyclerView() {
        binding.rvInputImage.adapter = adapter
    }

    private fun inputProductInfoForDB(): Boolean {
        var isCheck = true

        // 제품 이름 확인
        if (getProductName().isEmpty()) {
            binding.tfInputProductName.setErrorMessage("이름을 입력해주세요.")
            isCheck = false
        } else {
            binding.tfInputProductName.isErrorEnabled = false
        }

        // 제품 가격 확인
        if (getProductPrice().isEmpty()) {
            binding.tfInputProductPrice.setErrorMessage("가격을 입력해주세요")
            isCheck = false
        } else if (getProductPrice().toInt() > 2100000000) {
            binding.tfInputProductPrice.setErrorMessage("가격이 너무 높습니다")
            isCheck = false
        } else {
            binding.tfInputProductPrice.isErrorEnabled = false
        }

        // 저장 버튼 활성화 여부 설정
        binding.btnInputSave.isEnabled = isCheck

        return isCheck
    }

    private fun checkImageList() {
        binding.apply {
            if (imageList.isEmpty()) {
                groupInputNoImage.visibility = View.VISIBLE
                groupInputYesImage.visibility = View.INVISIBLE
            } else {
                groupInputNoImage.visibility = View.INVISIBLE
                groupInputYesImage.visibility = View.VISIBLE
            }
            binding.tvImageInputLabel.text = "사진 ( ${adapter.itemCount} / 3 )"
        }
    }

    private fun onClickSaveBtn() {
        binding.btnInputSave.apply {
            setOnClickListener {
                isEnabled = false
                settingDialog()
            }
        }
    }

    private fun getProductName(): String {
        return binding.tfInputProductName.editText?.text.toString()
    }

    private fun getProductPrice(): String {
        return binding.tfInputProductPrice.editText?.text.toString()
    }


    private fun savePhotoToDatabase() {
        val productName = getProductName()
        val productPrice = getProductPrice().toInt()
        val productLabel = binding.tfInputProductLabel.editText?.text.toString()
        //val productImages = imageList.map { it.imagePath }

        val productImages = imageList.mapNotNull { image ->
            val uri = Uri.parse(image.imagePath)
            val bitmap = loadBitmapFromUri(requireContext(), uri)
            bitmap?.let {
                val imageId = System.currentTimeMillis().toString() // 유니크 ID 생성
                ImageFileManager.saveImage(requireContext(), imageId, it) // 이미지 저장
                imageId // 저장된 이미지 ID 반환
            }
        }
        val productInfo = ProductInfo(
            productName = productName,
            productPrice = productPrice,
            productLabel = productLabel,
            productPath = productImages
        )
        val productDao = AppDatabase.getInstance(requireContext())?.productDao()
        lifecycleScope.launch(Dispatchers.IO) {
            productDao?.insert(productInfo)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "상품 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                moveToHomeFragment()
            }
        }
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream).also {
                inputStream?.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun onClickImageView() {
        binding.viewInputAddImage.setOnClickListener {
            openAlbum(isReplace = false)
        }
    }

    private fun openAlbum(isReplace: Boolean = false, pos: Int = -1) {
        Log.d("100", "openAlbum")
        if (isReplace) selectedImagePos = pos //교체 위치 저장
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        if (isReplace) {
            galleryLauncherChange.launch(intent)
        } else {
            galleryLauncherAdd.launch(intent)
        }
    }

    override fun onClickImage(pos: Int) {
        // 이미지 교체
        openAlbum(isReplace = true, pos)
    }

    private fun onClickCardView() {
        binding.cvInputImageAdd.setOnClickListener {
            if (imageList.size < 3) {
                openAlbum()
            } else {
                Toast.makeText(requireContext(), "사진은 최대3장까지 가능합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val galleryLauncherAdd = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                val image = Image(imagePath = it.toString())
                imageList.add(image)
                adapter.addImage(image)
                Log.d("TEST100", "$image")
            }
            checkImageList()
        }
    }

    private val galleryLauncherChange = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                if (selectedImagePos != -1) {
                    val image = Image(imagePath = it.toString())
                    adapter.changeImage(image, selectedImagePos)
                    selectedImagePos = -1
                    Log.d("TEST200", "$image")
                }
                checkImageList()
            }
        }
    }


    override fun onClickRemove(pos: Int) {
        imageList.removeAt(pos)
        adapter.removeImage(pos)
        checkImageList()
    }
}