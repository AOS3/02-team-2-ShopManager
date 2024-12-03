package com.lion.shopmanager.ui.show

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lion.shopmanager.databinding.ItemShowImageBinding
import com.lion.shopmanager.ui.extensions.fixOrientation
import com.lion.shopmanager.ui.extensions.rotate

class ShowImageAdapter(
) : RecyclerView.Adapter<PagerImageViewHolder>() {
    private val imageList = mutableListOf<Bitmap>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerImageViewHolder {
        val binding = ItemShowImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PagerImageViewHolder(binding)
    }

    fun submitList(image: List<Bitmap>) {
        imageList.clear()
        imageList.addAll(image)
        notifyDataSetChanged()
    }


    override fun onBindViewHolder(holder: PagerImageViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    override fun getItemCount(): Int {
        return imageList.size
    }
}


class PagerImageViewHolder(
    private val binding: ItemShowImageBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(bitmap: Bitmap) {
        try {
            // Bitmap의 방향을 바로잡기
            val correctedBitmap = bitmap.fixOrientation()
           
            Glide.with(binding.root.context)
                .load(correctedBitmap)
                .into(binding.ivItemShowImage)
        } catch (_: Exception) {

        }
    }
}