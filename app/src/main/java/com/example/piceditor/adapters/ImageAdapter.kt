package com.example.piceditor.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.piceditor.MyDraftActivity.formatDateTime
import com.example.piceditor.databinding.ItemImageSavedBinding
import com.example.piceditor.model.ImageModel

class ImageAdapter(
    private val list: List<ImageModel>
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: ItemImageSavedBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageSavedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = list[position]

        Glide.with(holder.itemView.context)
            .load(item.uri)
            .centerCrop()
            .into(holder.binding.img)

        holder.binding.tvSavedDate.text = formatDateTime(item.date)

    }

}