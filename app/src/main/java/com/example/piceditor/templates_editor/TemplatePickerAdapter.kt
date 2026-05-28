package com.example.piceditor.templates_editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.piceditor.R

class TemplatePickerAdapter(
    templates: List<TemplateData>,
    private val onPick: (TemplateData) -> Unit
) : RecyclerView.Adapter<TemplatePickerAdapter.VH>() {

    // ✅ Sắp xếp mới nhất trước (id lớn = thêm sau → lên đầu)
    private val templates: List<TemplateData> =
        templates.sortedByDescending { it.id.toIntOrNull() ?: 0 }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.imgThumb)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = templates[position]

        // ✅ Load qua Glide: decode ở background + downsample + cache → vuốt mượt
        //    RGB_565 (16-bit) + override nhỏ để giảm chất lượng cho nhẹ RAM
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
        // Kích thước decode thumbnail (tỉ lệ 9:16). Nhỏ hơn view thật để giảm
        // chất lượng + bộ nhớ, giúp vuốt mượt. Tăng nếu thấy ảnh bị mờ.
        private const val THUMB_W = 360
        private const val THUMB_H = 640
    }
}
