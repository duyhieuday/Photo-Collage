package com.example.piceditor.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.piceditor.R
import com.example.piceditor.model.ToolItem

class ToolAdapter(
    private val items: MutableList<ToolItem>,
    private val onClick: (ToolItem, Int) -> Unit
) : RecyclerView.Adapter<ToolAdapter.ToolViewHolder>() {

    private var selectedPosition = 0

    inner class ToolViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgIcon)
        val title: TextView = view.findViewById(R.id.txtTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tool, parent, false)
        return ToolViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val item = items[position]

        holder.icon.setImageResource(item.icon)
        holder.title.text = item.title

        // state selected
        holder.itemView.isSelected = position == selectedPosition

        holder.itemView.setOnClickListener {
            val oldPos = selectedPosition
            selectedPosition = position

            notifyItemChanged(oldPos)
            notifyItemChanged(selectedPosition)

            onClick(item, position)
        }
    }

    override fun getItemCount() = items.size
}