package com.example.piceditor.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.piceditor.DraftRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.piceditor.MyDraftActivity.formatDateTime
import com.example.piceditor.R
import com.example.piceditor.ads.InterAds
import com.example.piceditor.databinding.ItemImageDraftSavedBinding
import com.example.piceditor.databinding.ItemImageSavedBinding
import com.example.piceditor.model.ImageModel

class ImageDraftAdapter(
    private val list: MutableList<ImageModel>
) : RecyclerView.Adapter<ImageDraftAdapter.ImageViewHolder>() {

    private val selectedPositions = mutableSetOf<Int>()
    private var selectMode = false   // đổi tên để tránh conflict với setSelectMode()
    var onSelectionChanged: ((count: Int) -> Unit)? = null

    inner class ImageViewHolder(val binding: ItemImageDraftSavedBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageDraftSavedBinding.inflate(
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
                // ✅ Normal mode: mở lại đúng editor (collage/ai/edit/template) với ảnh đã lưu
                val context = holder.itemView.context
                InterAds.showAdsBreak(context as Activity?) {
                    val owner = context as? LifecycleOwner
                    if (owner != null) {
                        // IO (đọc project.json + copy file) chạy NỀN, startActivity ở main → tránh ANR
                        owner.lifecycleScope.launch {
                            val intent = withContext(Dispatchers.IO) {
                                DraftRouter.resolveIntent(context, item.uri)
                            }
                            context.startActivity(intent)
                        }
                    } else {
                        DraftRouter.open(context, item.uri)
                    }
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