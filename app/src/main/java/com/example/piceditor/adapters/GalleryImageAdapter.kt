package com.example.piceditor.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.piceditor.databinding.ItemImageBinding
import com.example.piceditor.utils.AndroidUtils

class GalleryImageAdapter(
    private val context: Context,
    private var images: ArrayList<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<GalleryImageAdapter.ViewHolder>() {

    private val selected = HashSet<String>()

    inner class ViewHolder(val binding: ItemImageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImageBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = images.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val path = images[position]

        // Load ảnh
        AndroidUtils.loadImageWithGlide(context, holder.binding.img, path)

        // Highlight nếu đã chọn
        holder.binding.overlay.visibility =
            if (selected.contains(path)) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {

            if (selected.contains(path)) {
                selected.remove(path)
            } else {
                selected.add(path)
            }

            notifyItemChanged(position)
            onClick(path)
        }
    }

    fun updateData(newList: ArrayList<String>) {
        images = newList
        notifyDataSetChanged()
    }
}