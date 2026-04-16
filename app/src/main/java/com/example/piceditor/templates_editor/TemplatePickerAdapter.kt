package com.example.piceditor.templates_editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.piceditor.R

class TemplatePickerAdapter(
    private val templates: List<TemplateData>,
    private val onPick: (TemplateData) -> Unit
) : RecyclerView.Adapter<TemplatePickerAdapter.VH>() {

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

        // ✅ Hiện thumbnail riêng (nhỏ, load nhanh)
        holder.thumb.setImageResource(t.thumbRes)
        holder.itemView.setOnClickListener { onPick(t) }
    }

    override fun getItemCount() = templates.size
}
