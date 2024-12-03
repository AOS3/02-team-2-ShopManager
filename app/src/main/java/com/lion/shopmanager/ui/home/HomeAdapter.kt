package com.lion.shopmanager.ui.home

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lion.shopmanager.R
import com.lion.shopmanager.data.ProductInfo
import com.lion.shopmanager.databinding.ItemHomeProductNameBinding
import com.lion.shopmanager.ui.extensions.fixOrientation
import com.lion.shopmanager.ui.extensions.formatToComma


class HomeAdapter(
    private val listener: HomeItemClickListener
) : RecyclerView.Adapter<HomeItemViewHolder>() {
    private val items = mutableListOf<ProductInfo>()
    private val imageList = mutableListOf<Bitmap>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeItemViewHolder {
        val binding = ItemHomeProductNameBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HomeItemViewHolder(binding, listener)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateItems(newItems: List<ProductInfo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun submitList(image: MutableList<Bitmap?>) {
        imageList.clear() // 기존 리스트 초기화
        imageList.addAll(image.filterNotNull()) // null 값을 제거한 리스트 추가
        notifyDataSetChanged() // RecyclerView 갱신
    }

    override fun onBindViewHolder(holder: HomeItemViewHolder, position: Int) {
        holder.bind(items[position], imageList[position])
    }
}

class HomeItemViewHolder(
    private val binding: ItemHomeProductNameBinding,
    private val listener: HomeItemClickListener
) : RecyclerView.ViewHolder(binding.root) {
    private var currentProduct: ProductInfo? = null

    init {
        binding.clRvHome.setOnClickListener {
            currentProduct?.let {
                listener.onClickProductName(it.idx)
            }
        }
    }

    fun bind(product: ProductInfo, bitmap: Bitmap?) {
        currentProduct = product

        // 텍스트 설정
        binding.tvRvHomeName.text = product.productName
        binding.tvRvHomePrice.text = product.productPrice.formatToComma(true)
        if (product.productSale) {
            binding.clRvHome.alpha = 1.0f // 판매 중이 아니면 투명도 제거
        } else {
            binding.clRvHome.alpha = 0.6f // 판매 중인 상품일 때 투명도 적용
        }
        try {
            // Bitmap의 방향을 바로잡기
            val correctedBitmap = bitmap?.fixOrientation()
            // 이미지 설정
            if (bitmap != null) {
                Glide.with(binding.root.context)
                    .load(correctedBitmap) // 비트맵 로드
                    .into(binding.ivRvHomeImage)
            } else {
                // 기본 이미지 설정
                Glide.with(binding.root.context)
                    .load(R.drawable.ic_no_image) // 기본 이미지
                    .into(binding.ivRvHomeImage)
            }
        } catch (_: Exception) {

        }

    }
}