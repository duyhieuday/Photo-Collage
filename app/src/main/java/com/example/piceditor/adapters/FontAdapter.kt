package com.example.piceditor.adapters

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.example.piceditor.R

data class FontItem(val name: String, val fontResId: Int)

class FontAdapter(
    private val context: Context,
    private val fonts: List<FontItem>,
    private val onFontSelected: (Typeface?) -> Unit
) : RecyclerView.Adapter<FontAdapter.VH>() {

    private var selectedPos = 0

    fun getSelectedTypeface(): Typeface? =
        ResourcesCompat.getFont(context, fonts[selectedPos].fontResId)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_font, parent, false)
        return VH(view)
    }

    override fun getItemCount() = fonts.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = fonts[position]
        val tf = ResourcesCompat.getFont(context, item.fontResId)
        holder.tvFont.typeface = tf
        holder.tvFont.isSelected = position == selectedPos

        holder.itemView.setOnClickListener {
            val old = selectedPos
            selectedPos = position
            notifyItemChanged(old)
            notifyItemChanged(selectedPos)
            onFontSelected(tf)
        }
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFont: AppCompatTextView = itemView.findViewById(R.id.tv_font)
    }
}