package com.lion.shopmanager.ui.modify

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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lion.shopmanager.R
import com.lion.shopmanager.data.AppDatabase
import com.lion.shopmanager.data.Image
import com.lion.shopmanager.data.ImageFileManager
import com.lion.shopmanager.data.ProductInfo
import com.lion.shopmanager.databinding.FragmentModifyBinding
import com.lion.shopmanager.ui.extensions.formatToComma
import com.lion.shopmanager.ui.extensions.setErrorMessage
import com.lion.shopmanager.ui.input.ImageItemClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ModifyFragment : Fragment(), ImageItemClickListener {
    private var _binding: FragmentModifyBinding? = null
    private val binding get() = _binding!!
    private val imageList = mutableListOf<Image>()
    private val bitmapList = mutableListOf<Bitmap>()
    private val adapter: ModifyImageAdapter by lazy { ModifyImageAdapter(this) }
    private var selectedImagePos: Int = -1


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getBundle()
        setRecyclerView()
        onClickCardView()
        maxLengthProductLabel()
        onClickImageView()
        settingToolbar()
        onClickSaveBtn()
    }

    private fun settingToolbar() {
        binding.toolbarModify.apply {
            setNavigationOnClickListener {
                moveToHomeFragment()
            }
            setOnMenuItemClickListener { itemId ->
                when (itemId.itemId) {
                    R.id.sale_on_modify_menu -> {
                        updateSaleDB(true)
                    }

                    R.id.sale_off_modify_menu -> {
                        updateSaleDB(false)
                    }
                }
                true
            }
        }
    }

    private fun updateSaleDB(isSale: Boolean) {
        val idx = arguments?.getInt("productIdx")!!
        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getInstance(requireContext())
            database?.productDao()?.updateSaleByIdx(isSale, idx)

            withContext(Dispatchers.Main) {
                if (isSale) {
                    Toast.makeText(requireContext(), "[판매 중]으로 변경 되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "[판매 완료]으로 변경 되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setRecyclerView() {
        binding.rvModifyImage.adapter = adapter
    }

    private fun moveToHomeFragment() {
        parentFragmentManager.popBackStack()
    }

    private fun getBundle() {
        val productIdx = arguments?.getInt("productIdx")!!
        Log.d("test", productIdx.toString())
        settingProductInfo(productIdx)
    }

    private fun maxLengthProductLabel() {
        binding.tfModifyProductLabel.editText?.addTextChangedListener { text ->
            if (text != null) {
                binding.tvModifyProductLabelCount.text = "( ${text.length} / 200)"
            }
        }
    }

    private fun settingProductInfo(productIdx: Int) {
        setLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getInstance(requireContext())?.productDao()
            val products = database?.getProductByIdx(productIdx)

            products?.forEach { product ->
                product.productPath.forEach { imageId ->
                    val bitmap = ImageFileManager.loadImage(requireContext(), imageId)
                    bitmap?.let {
                        bitmapList.add(it) // 비트맵 리스트 추가
                        imageList.add(Image(imagePath = imageId)) // 이미지 리스트 추가
                    }
                }
            }

            withContext(Dispatchers.Main) {
                products?.forEach { product ->
                    binding.apply {
                        tfModifyProductName.editText?.setText(product.productName)
                        tfModifyProductPrice.editText?.setText(product.productPrice.toString())
                        tfModifyProductLabel.editText?.setText(product.productLabel)
                    }
                    adapter.submitList(bitmapList) // 리사이클러뷰에 데이터 반영
                    checkImageList()
                    setLoading(false)
                }
            }
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

    private fun onClickCardView() {
        binding.cvModifyImageAdd.setOnClickListener {
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
                try {
                    // Uri를 Bitmap으로 변환
                    val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)

                    // 이미지 ID 생성 및 저장
                    val imageId = System.currentTimeMillis().toString()
                    ImageFileManager.saveImage(requireContext(), imageId, bitmap)

                    // 리스트 동기화
                    bitmapList.add(bitmap)
                    imageList.add(Image(imagePath = imageId))

                    // 어댑터에 Bitmap 추가
                    adapter.addBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "이미지 추가 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            checkImageList()
        }
    }

    private fun checkImageList() {
        binding.apply {
            if (imageList.isEmpty()) {
                groupModifyNoImage.visibility = View.VISIBLE
                groupModifyYesImage.visibility = View.INVISIBLE
            } else {
                groupModifyNoImage.visibility = View.INVISIBLE
                groupModifyYesImage.visibility = View.VISIBLE
            }
            binding.tvImageModifyLabel.text = "사진 ( ${adapter.itemCount} / 3 )"
        }
    }

    private val galleryLauncherChange = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                try {
                    // Uri를 Bitmap으로 변환
                    val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)

                    if (selectedImagePos != -1) {
                        // 기존 파일 삭제 후 새로운 이미지 저장
                        val oldImagePath = imageList[selectedImagePos].imagePath
                        ImageFileManager.deleteImage(requireContext(), oldImagePath)

                        val imageId = System.currentTimeMillis().toString()
                        ImageFileManager.saveImage(requireContext(), imageId, bitmap)

                        // 리스트 업데이트
                        bitmapList[selectedImagePos] = bitmap
                        imageList[selectedImagePos] = Image(imagePath = imageId)

                        // 어댑터에 변경된 Bitmap 반영
                        adapter.changeBitmap(bitmap, selectedImagePos)
                        selectedImagePos = -1
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "이미지 교체 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            checkImageList()
        }
    }

    private fun onClickSaveBtn() {
        binding.btnModifySave.apply {
            setOnClickListener {
                isEnabled = false
                settingDialog()
            }
        }
    }

    private fun inputProductInfoForDB(): Boolean {
        var isCheck = true

        // 제품 이름 확인
        if (getProductName().isEmpty()) {
            binding.tfModifyProductName.setErrorMessage("이름을 입력해주세요.")
            isCheck = false
        } else {
            binding.tfModifyProductName.isErrorEnabled = false
        }

        // 제품 가격 확인
        if (getProductPrice().isEmpty()) {
            binding.tfModifyProductPrice.setErrorMessage("가격을 입력해주세요")
            isCheck = false
        } else if (getProductPrice().toInt() > 2100000000) {
            binding.tfModifyProductPrice.setErrorMessage("가격이 너무 높습니다")
            isCheck = false
        } else {
            binding.tfModifyProductPrice.isErrorEnabled = false
        }

        // 저장 버튼 활성화 여부 설정
        binding.btnModifySave.isEnabled = isCheck

        return isCheck
    }

    private fun getProductName(): String {
        return binding.tfModifyProductName.editText?.text.toString()
    }

    private fun getProductPrice(): String {
        return binding.tfModifyProductPrice.editText?.text.toString()
    }


    private fun setLoading(loading: Boolean) {
        binding.pbModify.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.clLoadingModify.visibility = View.GONE
        } else {
            binding.clLoadingModify.visibility = View.VISIBLE
        }
    }


    private fun savePhotoToDatabase() {
        val productName = getProductName()
        val productPrice = getProductPrice().toInt()
        val productLabel = binding.tfModifyProductLabel.editText?.text.toString()

        // 저장된 imageList에서 경로 추출
        val productImages = imageList.map { it.imagePath }

        val productInfo = ProductInfo(
            productName = productName,
            productPrice = productPrice,
            productLabel = productLabel,
            productPath = productImages
        )

        val productDao = AppDatabase.getInstance(requireContext())?.productDao()
        lifecycleScope.launch(Dispatchers.IO) {
            val idx = arguments?.getInt("productIdx")!!
            productDao?.updateByIdx(
                productInfo.productName,
                productInfo.productPrice,
                productInfo.productLabel,
                productInfo.productPath,
                idx
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "상품 정보가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                moveToHomeFragment()
            }
        }
    }

    private fun settingDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.CustomMaterialDialogTheme)
        builder.setTitle("저장")
        builder.setMessage("저장하시겠습니까?")
        builder.setCancelable(false)
        builder.setNeutralButton("취소") { _: DialogInterface, _: Int ->
            binding.btnModifySave.isEnabled = true
        }

        builder.setPositiveButton("저장") { _: DialogInterface, _: Int ->
            if (inputProductInfoForDB()) {
                savePhotoToDatabase()
            } else {
                binding.btnModifySave.isEnabled = true
            }
        }
        builder.show()
    }

    private fun onClickImageView() {
        binding.viewModifyAddImage.setOnClickListener {
            openAlbum(isReplace = false)
        }
    }

    override fun onClickImage(pos: Int) {
        openAlbum(isReplace = true, pos)
    }

    override fun onClickRemove(pos: Int) {
        imageList.removeAt(pos)
        adapter.removeImage(pos)
        checkImageList()
    }
}