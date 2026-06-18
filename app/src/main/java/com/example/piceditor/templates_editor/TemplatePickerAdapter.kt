package com.example.piceditor.templates_editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.piceditor.R

/**
 * Adapter card dọc (9:16) dùng cho hàng cuộn ngang trong mỗi category.
 * Giữ nguyên thứ tự template do [TemplateRepository.sections] cung cấp (không tự sort).
 */
class TemplatePickerAdapter(
    private val templates: List<TemplateData>,
    private val itemLayout: Int = R.layout.item_template_card,
    private val onPick: (TemplateData) -> Unit
) : RecyclerView.Adapter<TemplatePickerAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.imgThumb)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(itemLayout, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = templates[position]

        // Preview dùng thumb_ (ảnh đã điền sẵn ảnh mẫu) cho card đẹp.
        // Load qua Glide: decode background + downsample + cache → vuốt mượt.
        // RGB_565 (16-bit) + override nhỏ để giảm RAM.
        Glide.with(holder.thumb)
            .load(t.thumbRes)
            .format(DecodeFormat.PREFER_RGB_565)
            .override(THUMB_W, THUMB_H)
            .centerCrop()
            .dontAnimate()
            .into(holder.thumb)

        holder.itemView.setOnClickListener { onPick(t) }
    }

    override fun getItemCount() = templates.size

    companion object {
        // Kích thước decode thumbnail (tỉ lệ 9:16). Card thật ~108x192dp nên
        // decode quanh kích thước đó là đủ nét mà vẫn nhẹ RAM.
        private const val THUMB_W = 324
        private const val THUMB_H = 576
    }
}
