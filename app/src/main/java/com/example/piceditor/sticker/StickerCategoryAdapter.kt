package com.example.piceditor.sticker

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.piceditor.R

class StickerCategoryAdapter(
    private var categories: List<StickerCategory>,
    private val onSelect: (Int, StickerCategory) -> Unit
) : RecyclerView.Adapter<StickerCategoryAdapter.VH>() {

    private var selectedIndex: Int = 0

    fun setSelected(index: Int) {
        if (index == selectedIndex) return
        val prev = selectedIndex
        selectedIndex = index
        notifyItemChanged(prev)
        notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sticker_category, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = categories[position]
        val selected = position == selectedIndex
        holder.root.setBackgroundResource(
            if (selected) R.drawable.bg_sticker_category_selected
            else R.drawable.bg_sticker_category
        )
        Glide.with(holder.image.context)
            .load(Uri.parse(cat.preview.assetPath))
            .into(holder.image)
        holder.root.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            setSelected(pos)
            onSelect(pos, categories[pos])
        }
    }

    override fun getItemCount(): Int = categories.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val root: FrameLayout = view.findViewById(R.id.categoryRoot)
        val image: AppCompatImageView = view.findViewById(R.id.categoryImage)
    }
}
