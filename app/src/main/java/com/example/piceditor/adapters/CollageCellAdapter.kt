package com.example.piceditor.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.piceditor.R
import com.example.piceditor.frame.FrameImageView

/**
 * Danh sách thumbnail từng ô ảnh trong collage cho tab Replace — giống PhotoCellAdapter của template.
 * Tap 1 ô → callback để mở gallery thay ảnh ô đó.
 */
class CollageCellAdapter(
    private val cells: List<FrameImageView>,
    private val onCellClick: (FrameImageView) -> Unit
) : RecyclerView.Adapter<CollageCellAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgPreview: ImageView = view.findViewById(R.id.imgCellPreview)
        val tvIndex: TextView = view.findViewById(R.id.tvCellIndex)
        val container: FrameLayout = view.findViewById(R.id.cellContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_cell, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cell = cells[position]

        holder.tvIndex.text = (position + 1).toString()

        val bmp = cell.image
        if (bmp != null && !bmp.isRecycled) {
            holder.imgPreview.setImageBitmap(bmp)
            holder.imgPreview.visibility = View.VISIBLE
        } else {
            holder.imgPreview.setImageDrawable(null)
            holder.imgPreview.visibility = View.GONE
        }

        val border = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setStroke(3, if (bmp != null) Color.parseColor("#0099FF") else Color.GRAY)
            cornerRadius = 8f
            setColor(Color.parseColor("#F5F5F5"))
        }
        holder.container.background = border

        holder.itemView.setOnClickListener { onCellClick(cell) }
    }

    override fun getItemCount() = cells.size
}
