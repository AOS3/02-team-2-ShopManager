package com.lion.shopmanager.ui.modify

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lion.shopmanager.databinding.ItemInputImageBinding
import com.lion.shopmanager.ui.extensions.fixOrientation
import com.lion.shopmanager.ui.extensions.rotate
import com.lion.shopmanager.ui.input.ImageItemClickListener

class ModifyImageAdapter(
    private val listener: ImageItemClickListener
) : RecyclerView.Adapter<ImageViewHolder>() {
    private val bitmapList = mutableListOf<Bitmap>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemInputImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ImageViewHolder(binding, listener)
    }

    override fun getItemCount(): Int {
        return bitmapList.size
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(bitmapList[position])
    }

    fun submitList(image: List<Bitmap>) {
        bitmapList.clear()
        bitmapList.addAll(image)
        notifyDataSetChanged()
    }

    fun addBitmap(bitmap: Bitmap) {
        bitmapList.add(bitmap)
        notifyItemInserted(bitmapList.size - 1) // 리스트에 추가된 항목 업데이트
    }

    fun removeImage(pos: Int) {
        if (pos in bitmapList.indices) {
            bitmapList.removeAt(pos)
            notifyItemRemoved(pos)
            notifyItemRangeChanged(pos, bitmapList.size)
        }
    }

    fun changeBitmap(bitmap: Bitmap, pos: Int) {
        if (pos in bitmapList.indices) {
            bitmapList[pos] = bitmap
            notifyItemChanged(pos) // 해당 위치 업데이트
        }
    }

    fun getImageList(): List<Bitmap?> {
        return bitmapList
    }

}


class ImageViewHolder(
    private val binding: ItemInputImageBinding,
    private val listener: ImageItemClickListener
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(bitmap: Bitmap) {
        try {
            // Bitmap의 방향을 바로잡기
            val correctedBitmap = bitmap.fixOrientation()

            // Glide를 사용하여 Bitmap 로드
            Glide.with(binding.root.context)
                .load(correctedBitmap)
                .into(binding.ivItemInputImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.ivItemInputImage.setOnClickListener {
            listener.onClickImage(adapterPosition)
        }
        binding.ivItemInputRemoveImage.setOnClickListener {
            listener.onClickRemove(adapterPosition)
        }
    }
}