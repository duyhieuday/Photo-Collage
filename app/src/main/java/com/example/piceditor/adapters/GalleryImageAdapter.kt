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

    // ✅ Dùng Map để đếm số lần mỗi ảnh được chọn
    private val selectedCount = mutableMapOf<String, Int>()

    inner class ViewHolder(val binding: ItemImageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImageBinding.inflate(
            LayoutInflater.from(context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = images.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val path = images[position]

        AndroidUtils.loadImageWithGlide(context, holder.binding.img, path)

        val count = selectedCount[path] ?: 0

        if (count > 0) {
            holder.binding.overlay.visibility = View.VISIBLE
            holder.binding.tvCount.visibility = View.VISIBLE
            holder.binding.tvCount.text = count.toString()
        } else {
            holder.binding.overlay.visibility = View.GONE
            holder.binding.tvCount.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            // ✅ Không tăng count ở đây — để activity quyết định có cho chọn không
            // Activity sẽ gọi increaseCount() nếu hợp lệ
            onClick(path)
        }
    }

    // ✅ Activity gọi sau khi đã confirm cho phép chọn
    fun increaseCount(path: String) {
        selectedCount[path] = (selectedCount[path] ?: 0) + 1
        val position = images.indexOf(path)
        if (position >= 0) notifyItemChanged(position)
    }

    // ✅ Gọi từ SelectImageActivity khi user xóa ảnh khỏi danh sách
    fun decreaseCount(path: String) {
        val current = selectedCount[path] ?: 0
        if (current <= 1) {
            selectedCount.remove(path)
        } else {
            selectedCount[path] = current - 1
        }
        val position = images.indexOf(path)
        if (position >= 0) notifyItemChanged(position)
    }

    // ✅ Reset toàn bộ khi cần
    fun clearSelection() {
        selectedCount.clear()
        notifyDataSetChanged()
    }

    fun updateData(newList: ArrayList<String>) {
        images = newList
        notifyDataSetChanged()
    }
}