package com.lion.shopmanager.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.internal.TextWatcherAdapter
import com.lion.shopmanager.R
import com.lion.shopmanager.data.AppDatabase
import com.lion.shopmanager.data.Image
import com.lion.shopmanager.data.ImageFileManager
import com.lion.shopmanager.data.ProductInfo
import com.lion.shopmanager.databinding.FragmentHomeBinding
import com.lion.shopmanager.ui.extensions.hideKeyboard
import com.lion.shopmanager.ui.extensions.showKeyboard
import com.lion.shopmanager.ui.input.InputFragment
import com.lion.shopmanager.ui.show.ShowFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class HomeFragment : Fragment(), HomeItemClickListener {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val productList = mutableListOf<ProductInfo>()
    private var searchList = mutableListOf<ProductInfo>()
    private var navigationList = mutableListOf<ProductInfo>()
    private val imageList = mutableListOf<Image>()

    companion object {
        private const val REQUEST_CODE_READ_MEDIA_IMAGES = 101
        private const val REQUEST_CODE_READ_EXTERNAL_STORAGE = 102
    }

    private val adapter: HomeAdapter by lazy { HomeAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        checkAndRequestStoragePermission()
        settingToolbar()
        onClickSearchViewClose()
        settingRecyclerView()
        onClickFab()
        backOnClick()
        getAllDB()
        searchProductName()
        settingSideNavigationView()
    }


    private fun checkAndRequestStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한 요청
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_READ_EXTERNAL_STORAGE
            )
        } else {
            // 권한이 이미 허용된 경우
            onStoragePermissionGranted()
        }
    }

    private fun onStoragePermissionGranted() {
        Log.d("Permission", "READ_EXTERNAL_STORAGE 권한 허용됨")
    }

    private fun onStoragePermissionDenied() {
        Log.e("Permission", "READ_EXTERNAL_STORAGE 권한 거부됨")
        // 권한이 없으면 사용자에게 알림 표시
        binding.root.context.let {
            androidx.appcompat.app.AlertDialog.Builder(it)
                .setTitle("권한 필요")
                .setMessage("이미지를 불러오려면 저장소 접근 권한이 필요합니다.")
                .setPositiveButton("설정") { _, _ ->
                    // 설정 화면으로 이동
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.fromParts("package", requireContext().packageName, null)
                    startActivity(intent)
                }
                .setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 허용됨
                onStoragePermissionGranted()
            } else {
                // 권한 거부됨
                onStoragePermissionDenied()
            }
        }
    }


    private fun onClickSearchViewClose() {
        binding.ivSearchCloseHome.setOnClickListener {
            binding.tfSearchHome.editText?.setText("")
            val groupViews = binding.groupSearchViewHome.referencedIds.map { id ->
                binding.root.findViewById<View>(id)
            }
            if (binding.groupSearchViewHome.visibility == View.VISIBLE) {
                // 숨김 애니메이션
                hideKeyboard()
                val slideUp = AnimationUtils.loadAnimation(context, R.anim.search_view_up_anim)
                groupViews.forEach { view ->
                    view.startAnimation(slideUp)
                }
                slideUp.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        groupViews.forEach { it.visibility = View.GONE }
                        binding.groupSearchViewHome.visibility = View.GONE
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                })
            } else {
                // 표시 애니메이션
                showKeyboard(binding.tfSearchHome.editText!!)
                val slideDown = AnimationUtils.loadAnimation(context, R.anim.search_view_down_anim)
                binding.groupSearchViewHome.visibility = View.VISIBLE
                groupViews.forEach { view ->
                    view.visibility = View.VISIBLE
                    view.startAnimation(slideDown)
                }
            }
            adapter.updateItems(productList)
        }
    }

    private fun getAllDB() {
        setLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val products = AppDatabase.getInstance(requireContext())?.productDao()?.getAllProduct()

            val bitmapList = mutableListOf<Bitmap?>() // Null 허용 리스트
            products?.forEach { product ->
                val firstImagePath = product.productPath.firstOrNull()
                val bitmap = firstImagePath?.let { ImageFileManager.loadImage(requireContext(), it) }
                    ?: getDefaultBitmap(requireContext()) // Null이면 기본 이미지를 추가
                bitmapList.add(bitmap)
            }

            withContext(Dispatchers.Main) {
                productList.clear()
                productList.addAll(products ?: emptyList())
                adapter.updateItems(productList)
                adapter.submitList(bitmapList) // 비트맵 리스트 전달
                setLoading(false)
            }
        }
    }

    private fun getDefaultBitmap(context: Context): Bitmap {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_no_image)
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        } else if (drawable != null) {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // 빈 비트맵 반환
    }

    private fun setLoading(loading: Boolean) {
        binding.pbHome.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.clLoadingHome.visibility = View.GONE
        } else {
            binding.clLoadingHome.visibility = View.VISIBLE
        }
    }

    private fun searchProductName() {
        binding.tfSearchHome.editText?.addTextChangedListener(
            @SuppressLint("RestrictedApi")
            object : TextWatcherAdapter() {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.toString().isEmpty()) {
                        binding.rvHome.visibility = View.VISIBLE
                        binding.tvHomeNoSearch.visibility = View.GONE
                        adapter.updateItems(productList)
                        adapter.submitList(productList.map {
                            val firstImagePath = it.productPath.firstOrNull()
                            firstImagePath?.let { path -> ImageFileManager.loadImage(requireContext(), path) }
                                ?: getDefaultBitmap(requireContext())
                        }.toMutableList())
                    } else {
                        searchResultByAdapter(s.toString())
                    }
                }
            }
        )
    }

    private fun searchResultByAdapter(str: String) {
        searchList.clear()
        val filteredProducts = productList.filter { it.productName.contains(str, ignoreCase = true) }
        searchList.addAll(filteredProducts)

        val filteredBitmaps = filteredProducts.map { product ->
            val firstImagePath = product.productPath.firstOrNull()
            firstImagePath?.let { ImageFileManager.loadImage(requireContext(), it) }
                ?: getDefaultBitmap(requireContext()) // 기본 이미지를 가져옵니다.
        }

        if (searchList.isEmpty()) {
            binding.apply {
                rvHome.visibility = View.INVISIBLE
                tvHomeNoSearch.apply {
                    text = "\"${str}\" \n 검색결과가 없습니다."
                    visibility = View.VISIBLE
                }
            }
        } else {
            binding.tvHomeNoSearch.visibility = View.GONE
            binding.rvHome.visibility = View.VISIBLE
            adapter.updateItems(searchList)
            adapter.submitList(filteredBitmaps.toMutableList())
        }
    }


    private fun settingRecyclerView() {
        binding.rvHome.adapter = adapter
    }

    private fun onClickFab() {
        binding.fabHomeAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(
                    R.anim.slide_in_right_anim,  // enter 애니메이션
                    R.anim.slide_out_left_anim,  // exit 애니메이션
                    R.anim.slide_in_left_anim,   // popEnter 애니메이션 (뒤로 가기)
                    R.anim.slide_out_right_anim  // popExit 애니메이션 (뒤로 가기)
                )
                .replace(R.id.fragment_container_main, InputFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun backOnClick() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.drawerlayoutHome.isDrawerOpen(GravityCompat.START)) {
                        binding.drawerlayoutHome.closeDrawer(GravityCompat.START)
                    } else {
                        // 기본 뒤로 가기 동작 수행
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun settingSideNavigationView() {
        binding.navigationViewHome.setNavigationItemSelectedListener { menuId ->
            when (menuId.itemId) {
                R.id.show_all -> {
                    binding.toolbarHome.title = "상품 리스트"
                    binding.groupSaleHome.visibility = View.GONE
                    adapter.updateItems(productList)
                }

                R.id.show_sale_on -> {
                    getSaleItem(true)
                }

                R.id.show_sale_off -> {
                    getSaleItem(false)
                }
            }
            // 메뉴 닫기
            binding.drawerlayoutHome.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun getSaleItem(isSale: Boolean) {
        navigationList = productList.filter { it.productSale == isSale }.toMutableList()
        adapter.updateItems(navigationList)
        if (isSale) {
            binding.toolbarHome.title = "판매 중 리스트"
        } else {
            binding.toolbarHome.title = "판매 완료 리스트"
        }
        if (navigationList.isEmpty()) {
            binding.groupSaleHome.visibility = View.VISIBLE
        } else {
            binding.groupSaleHome.visibility = View.GONE
        }
    }

    private fun settingToolbar() {
        binding.toolbarHome.apply {
            setNavigationOnClickListener {
                binding.drawerlayoutHome.open()
            }
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.search_home_menu -> {
                        val groupViews = binding.groupSearchViewHome.referencedIds.map { id ->
                            binding.root.findViewById<View>(id)
                        }

                        if (binding.groupSearchViewHome.visibility == View.VISIBLE) {
                            hideKeyboard()
                            // 숨김 애니메이션
                            val slideUp = AnimationUtils.loadAnimation(context, R.anim.search_view_up_anim)
                            groupViews.forEach { view ->
                                view.startAnimation(slideUp)
                            }
                            slideUp.setAnimationListener(object : Animation.AnimationListener {
                                override fun onAnimationStart(animation: Animation?) {}
                                override fun onAnimationEnd(animation: Animation?) {
                                    groupViews.forEach { it.visibility = View.GONE }
                                    binding.groupSearchViewHome.visibility = View.GONE
                                }

                                override fun onAnimationRepeat(animation: Animation?) {}
                            })
                        } else {
                            showKeyboard(binding.tfSearchHome.editText!!)
                            // 표시 애니메이션
                            val slideDown = AnimationUtils.loadAnimation(context, R.anim.search_view_down_anim)
                            binding.groupSearchViewHome.visibility = View.VISIBLE
                            groupViews.forEach { view ->
                                view.visibility = View.VISIBLE
                                view.startAnimation(slideDown)
                            }
                        }
                    }
                }
                true
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun moveToShowFragment(productIdx: Int) {
        val fragment = ShowFragment()
        val bundle = Bundle()
        bundle.putInt("productIdx", productIdx) // 클릭된 idx를 번들에 추가
        Log.d("ProductIdx: ", "$productIdx")
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
            .addToBackStack(null)
            .commit()
    }


    override fun onClickProductName(productIdx: Int) {
        moveToShowFragment(productIdx)
    }
}