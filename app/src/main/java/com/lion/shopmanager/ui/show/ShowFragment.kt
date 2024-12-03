package com.lion.shopmanager.ui.show

import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lion.shopmanager.R
import com.lion.shopmanager.data.AppDatabase
import com.lion.shopmanager.data.Image
import com.lion.shopmanager.data.ImageFileManager
import com.lion.shopmanager.databinding.FragmentShowBinding
import com.lion.shopmanager.ui.extensions.formatToComma
import com.lion.shopmanager.ui.modify.ModifyFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ShowFragment : Fragment() {
    private var _binding: FragmentShowBinding? = null
    private val binding get() = _binding!!

    private val adapter: ShowImageAdapter by lazy { ShowImageAdapter() }
    private val imageList = mutableListOf<Image>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar()
        onClickModifyBtn()
        getBundle()
        selectImage()
    }

    private fun getBundle() {
        val productIdx = arguments?.getInt("productIdx")
        setProductInfo(productIdx!!)
    }

    private fun setToolbar() {
        binding.toolbarModify.apply {
            setOnMenuItemClickListener { menuId ->
                when (menuId.itemId) {
                    R.id.delete_show_menu -> {
                        // DB제거
                        settingDialog()
                    }
                }
                true
            }
            setNavigationOnClickListener {
                moveToHomeFragment()
            }
        }
    }

    private fun deleteRoomDB() {
        val productIdx = arguments?.getInt("productIdx")!!
        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getInstance(requireContext())?.productDao()
            database?.deleteByIdx(productIdx)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "삭제 되었습니다.", Toast.LENGTH_SHORT).show()
                moveToHomeFragment()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.pbShow.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.clLoadingShow.visibility = View.GONE
        } else {
            binding.clLoadingShow.visibility = View.VISIBLE
        }
    }

    private fun setProductInfo(productIdx: Int) {
        setLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getInstance(requireContext())?.productDao()
            val products = database?.getProductByIdx(productIdx)
            Log.d("TEST","${products}")
            val bitmapList = mutableListOf<Bitmap>()

            products?.forEach { product ->
                product.productPath.forEach { imageId ->
                    val bitmap = ImageFileManager.loadImage(requireContext(), imageId)
                    bitmap?.let { bitmapList.add(it) }
                }
            }

            withContext(Dispatchers.Main) {
                products?.forEach { product ->
                    binding.apply {
                        tfShowProductName.editText?.setText(product.productName)
                        tfShowProductPrice.editText?.setText(product.productPrice.formatToComma(true))
                        tfShowProductLabel.editText?.setText(product.productLabel)
                    }
                    product.productPath.forEach { path ->
                        val image = Image(imagePath = path)
                        imageList.add(image)
                    }
                }
                adapter.submitList(bitmapList)
                setViewPagerAdapter()
                noImage()
                setLoading(false)
            }
        }
    }

    private fun onClickModifyBtn() {
        binding.btnShowModify.setOnClickListener {
            binding.btnShowModify.isEnabled = false
            moveToModifyFragment()
        }
    }

    private fun setViewPagerAdapter() {
        binding.vpShow.adapter = adapter
    }

    private fun selectImage() {
        val arrowList = listOf(binding.ivShowBack, binding.ivShowForward)
        arrowList.forEachIndexed { index, imageButton ->
            imageButton.setOnClickListener {
                val currentItem = binding.vpShow.currentItem
                val itemCount = adapter.itemCount
                when (index) {
                    0 -> {
                        if (currentItem > 0) {
                            binding.vpShow.setCurrentItem(currentItem - 1, true)
                        }
                    }

                    1 -> {
                        if (currentItem < itemCount - 1) {
                            binding.vpShow.setCurrentItem(currentItem + 1, true) // 다음 페이지로 이동
                        }
                    }
                }
            }
        }
    }

    private fun moveToModifyFragment() {
        val fragment = ModifyFragment()
        val bundle = Bundle()
        val productIdx = arguments?.getInt("productIdx")!!
        bundle.putInt("productIdx", productIdx)
        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                R.anim.slide_in_right_anim,  // enter 애니메이션
                R.anim.slide_out_left_anim,  // exit 애니메이션
                R.anim.slide_in_left_anim,   // popEnter 애니메이션 (뒤로 가기)
                R.anim.slide_out_right_anim  // popExit 애니메이션 (뒤로 가기)
            )
            .replace(R.id.fragment_container_main, fragment)
            .addToBackStack("ShowFragment")
            .commit()
    }

    private fun noImage() {
        val imageCount = adapter.itemCount
        Log.d("test", imageCount.toString())
        if (imageCount == 0) {
            binding.groupShowNoImage.visibility = View.VISIBLE
            binding.groupShowYesImage.visibility = View.INVISIBLE
            binding.groupShowArrow.visibility = View.INVISIBLE
        } else {
            binding.groupShowYesImage.visibility = View.VISIBLE
            binding.groupShowArrow.visibility = View.VISIBLE
            binding.groupShowNoImage.visibility = View.INVISIBLE
        }
    }


    private fun settingDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.CustomMaterialDialogTheme)
        builder.setTitle("삭제")
        builder.setMessage("선택한 제품을 삭제하시겠습니까?\n삭제 후 되돌릴 수 없습니다!!")
        builder.setNeutralButton("취소", null)
        builder.setPositiveButton("삭제") { _: DialogInterface, _: Int ->
            deleteRoomDB()
        }
        builder.show()
    }

    private fun moveToHomeFragment() {
        parentFragmentManager.popBackStack()
    }
}
