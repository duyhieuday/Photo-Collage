package com.example.piceditor.adapters

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.piceditor.MyDraftActivity.formatDateTime
import com.example.piceditor.R
import com.example.piceditor.SelectImageActivity
import com.example.piceditor.ShowImageActivity
import com.example.piceditor.ads.InterAds
import com.example.piceditor.databinding.ItemImageSavedBinding
import com.example.piceditor.model.ImageModel

class ImageAdapter(
    private val list: MutableList<ImageModel>
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    private val selectedPositions = mutableSetOf<Int>()
    private var selectMode = false   // đổi tên để tránh conflict với setSelectMode()
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

        holder.binding.root.setBackgroundResource(
            if (selectMode && isSelected) R.drawable.bg_saved_image_selected
            else R.drawable.bg_saved_image
        )

        holder.binding.root.setOnClickListener {
            if (selectMode) {
                // ── Select mode: toggle chọn item ──
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position)
                } else {
                    selectedPositions.add(position)
                }
                notifyItemChanged(position)
                onSelectionChanged?.invoke(selectedPositions.size)
            } else {
                // ✅ Normal mode: mở ShowImageActivity với URI của ảnh
                val context = holder.itemView.context
                InterAds.showAdsBreak(context as Activity?) {
                    context.startActivity(
                        Intent(context, ShowImageActivity::class.java).apply {
                            putExtra("image_uri", item.uri.toString())
                        }
                    )
                }
            }
        }

        // ✅ Long press → bật select mode và chọn item này luôn
        holder.binding.root.setOnLongClickListener {
            if (!selectMode) {
                selectMode = true
                selectedPositions.add(position)
                notifyDataSetChanged()
                onSelectionChanged?.invoke(selectedPositions.size)
            }
            true
        }
    }

    fun setSelectMode(enabled: Boolean) {
        selectMode = enabled
        if (!enabled) {
            selectedPositions.clear()
        }
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<ImageModel> = selectedPositions.map { list[it] }

    fun getSelectedCount() = selectedPositions.size

    fun deleteSelected() {
        selectedPositions.sortedDescending().forEach { list.removeAt(it) }
        selectedPositions.clear()
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedPositions.clear()
        selectMode = false
        notifyDataSetChanged()
    }
}