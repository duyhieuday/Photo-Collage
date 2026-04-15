package com.example.piceditor.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.piceditor.MyDraftActivity.formatDateTime
import com.example.piceditor.R
import com.example.piceditor.databinding.ItemImageSavedBinding
import com.example.piceditor.model.ImageModel

class ImageAdapter(
    private val list: MutableList<ImageModel>
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    private val selectedPositions = mutableSetOf<Int>()
    var isSelectMode = false
    var onSelectionChanged: ((count: Int) -> Unit)? = null

    inner class ImageViewHolder(val binding: ItemImageSavedBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageSavedBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
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

        val isSelected = selectedPositions.contains(position)
        holder.binding.checkbox.isVisible = isSelectMode
        holder.binding.checkbox.isChecked = isSelected

        // Highlight border khi selected
        holder.binding.root.setBackgroundResource(
            if (isSelectMode && isSelected) R.drawable.bg_saved_image_selected
            else R.drawable.bg_saved_image
        )

        holder.binding.root.setOnClickListener {
            if (isSelectMode) {
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position)
                } else {
                    selectedPositions.add(position)
                }
                notifyItemChanged(position)
                // Chỉ update count, không show dialog
                onSelectionChanged?.invoke(selectedPositions.size)
            }
        }
    }

    fun getSelectedItems(): List<ImageModel> {
        return selectedPositions.map { list[it] }
    }

    fun getSelectedCount() = selectedPositions.size

    fun deleteSelected() {
        selectedPositions.sortedDescending().forEach { list.removeAt(it) }
        selectedPositions.clear()
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedPositions.clear()
        isSelectMode = false
        notifyDataSetChanged()
    }
}