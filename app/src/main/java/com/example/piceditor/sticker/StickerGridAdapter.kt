package com.example.piceditor.sticker

import android.app.Activity
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.piceditor.R
import com.example.piceditor.WeatherApplication
import com.example.piceditor.ads.iap.PremiumUpsell

class StickerGridAdapter(
    private val onPick: (StickerItem) -> Unit
) : RecyclerView.Adapter<StickerGridAdapter.VH>() {

    private var items: List<StickerItem> = emptyList()
    private var selectedIndex: Int = -1

    fun submit(newItems: List<StickerItem>) {
        items = newItems
        selectedIndex = -1
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sticker_grid, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.root.setBackgroundResource(
            if (position == selectedIndex) R.drawable.bg_sticker_item_selected
            else R.drawable.bg_sticker_item
        )
        Glide.with(holder.image.context)
            .load(Uri.parse(item.assetPath))
            .into(holder.image)

        // Soft-sell gate: sticker premium + user free → badge 👑 + dialog mời Premium ("Continue" vẫn cho dùng)
        val ctx = holder.itemView.context
        val locked = item.isPremium && !PremiumUpsell.isPremium(ctx)
        holder.pro.visibility = if (locked) View.VISIBLE else View.GONE
        holder.root.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val pick = {
                WeatherApplication.trackingEvent(
                    "select_sticker", "sticker", item.assetPath.substringAfterLast('/')
                )
                val prev = selectedIndex
                selectedIndex = pos
                if (prev != -1) notifyItemChanged(prev)
                notifyItemChanged(pos)
                onPick(items[pos])
            }
            val act = ctx as? Activity
            if (locked && act != null) PremiumUpsell.showFeatureDialog(act) { pick() }
            else pick()
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val root: FrameLayout = view.findViewById(R.id.stickerRoot)
        val image: AppCompatImageView = view.findViewById(R.id.stickerImage)
        val pro: ImageView = view.findViewById(R.id.stickerPro)
    }
}
