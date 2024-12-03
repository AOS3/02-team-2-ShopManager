package com.lion.shopmanager.ui.input

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lion.shopmanager.data.Image
import com.lion.shopmanager.databinding.ItemInputImageBinding

class InputImageAdapter(
    private val listener: ImageItemClickListener
) : RecyclerView.Adapter<ImageViewHolder>() {
    private val imageList = mutableListOf<Image>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemInputImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ImageViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    fun addImage(newImage: Image) {
        imageList.add(newImage)
        notifyItemInserted(imageList.size - 1) // 마지막 위치에 삽입 알림
    }

    fun changeImage(changeImage: Image, pos: Int) {
        if (pos in imageList.indices) {
            imageList[pos] = changeImage
            notifyItemChanged(pos) // 해당 위치만 갱신
        }
    }

    fun removeImage(pos: Int) {
        if (pos in imageList.indices) {
            imageList.removeAt(pos)
            notifyItemRemoved(pos)
            notifyItemRangeChanged(pos, imageList.size)
        }
    }
}

class ImageViewHolder(
    private val binding: ItemInputImageBinding,
    private val listener: ImageItemClickListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(image: Image) {
        try {
            Glide.with(binding.root.context)
                .load(image.imagePath)
                .into(binding.ivItemInputImage)
        } catch (_: Exception) {

        }
        binding.ivItemInputImage.setOnClickListener {
            listener.onClickImage(adapterPosition) // 이미 사진이 있으면 변경
        }
        binding.ivItemInputRemoveImage.setOnClickListener {
            listener.onClickRemove(adapterPosition) // 사진 제거
        }
//        binding.ivItemInputImage.setImageURI(imagePath.imagePath.toUri())
    }
}
