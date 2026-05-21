package com.example.piceditor.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.piceditor.R

class ColorAdapter(
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorAdapter.VH>() {

    // Dãy màu mặc định
    private val colors = listOf(
        Color.parseColor("#000000"),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#F04438"),
        Color.parseColor("#FB6514"),
        Color.parseColor("#F79009"),
        Color.parseColor("#EAAA08"),
        Color.parseColor("#66C61C"),
        Color.parseColor("#12B76A"),
        Color.parseColor("#06AED4"),
        Color.parseColor("#2E90FA"),
        Color.parseColor("#6172F3"),
        Color.parseColor("#7A5AF8"),
        Color.parseColor("#9E77ED"),
        Color.parseColor("#EE46BC"),
    )

    private var selectedPos = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_brush_color, parent, false)
        return VH(view)
    }

    override fun getItemCount() = colors.size

    override fun onBindViewHolder(holder: VH, @SuppressLint("RecyclerView") position: Int) {
        val color = colors[position]

        // Chấm màu — bo tròn
        val dot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            // màu trắng thì thêm viền xám cho thấy
            if (color == Color.WHITE) {
                setStroke(2, Color.parseColor("#D0D5DD"))
            }
        }
        holder.colorDot.background = dot

        // Vòng chọn
        holder.colorRing.visibility =
            if (position == selectedPos) View.VISIBLE else View.INVISIBLE

        holder.itemView.setOnClickListener {
            val old = selectedPos
            selectedPos = position
            notifyItemChanged(old)
            notifyItemChanged(selectedPos)
            onColorSelected(color)
        }
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorDot: View  = itemView.findViewById(R.id.color_dot)
        val colorRing: View = itemView.findViewById(R.id.color_ring)
    }
}