package com.example.piceditor.templates_editor

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.piceditor.R

class PhotoCellAdapter(
    private val cells: List<PhotoCell>,
    private val onCellClick: (PhotoCell) -> Unit
) : RecyclerView.Adapter<PhotoCellAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgPreview: ImageView = view.findViewById(R.id.imgCellPreview)
        val tvIndex: TextView     = view.findViewById(R.id.tvCellIndex)
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

        if (cell.bitmap != null) {
            // Có ảnh → hiện preview
            holder.imgPreview.setImageBitmap(cell.bitmap)
            holder.imgPreview.visibility = View.VISIBLE
        } else {
            // Chưa có ảnh → hiện placeholder "+"
            holder.imgPreview.setImageDrawable(null)
            holder.imgPreview.visibility = View.GONE
        }

        // Border highlight
        val border = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setStroke(3, if (cell.bitmap != null) Color.parseColor("#0099FF") else Color.GRAY)
            cornerRadius = 8f
            setColor(Color.parseColor("#F5F5F5"))
        }
        holder.container.background = border

        holder.itemView.setOnClickListener { onCellClick(cell) }
    }

    override fun getItemCount() = cells.size
}