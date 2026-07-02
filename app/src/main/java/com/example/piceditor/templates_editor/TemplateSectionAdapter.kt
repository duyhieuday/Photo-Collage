package com.example.piceditor.templates_editor

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piceditor.R
import com.example.piceditor.WeatherApplication
import com.example.piceditor.ads.InterAds

/**
 * Adapter dọc cho màn picker: mỗi item là 1 category gồm header (emoji + tên +
 * mũi tên) và 1 hàng RecyclerView cuộn ngang các template thuộc category đó.
 */
class TemplateSectionAdapter(
    private val sections: List<TemplateRepository.CategorySection>,
    private val onPick: (TemplateData) -> Unit
) : RecyclerView.Adapter<TemplateSectionAdapter.VH>() {

    // Chia sẻ view pool giữa các hàng ngang → tái dùng card, cuộn mượt hơn.
    private val sharedPool = RecyclerView.RecycledViewPool()

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvCategoryTitle)
        val seeAll: View = view.findViewById(R.id.btnSeeAll)
        val rv: RecyclerView = view.findViewById(R.id.rvCategory)

        init {
            rv.layoutManager =
                LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
            rv.setRecycledViewPool(sharedPool)
            rv.setHasFixedSize(true)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template_section, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val section = sections[position]
        holder.title.text = section.title
        holder.rv.adapter = TemplatePickerAdapter(section.templates, onPick = onPick)

        // "See all" / tên category → mở màn full-category (kèm inter ad như các transition khác).
        holder.seeAll.setOnClickListener { openCategory(holder.itemView.context, section) }
        holder.title.setOnClickListener { openCategory(holder.itemView.context, section) }
    }

    private fun openCategory(ctx: android.content.Context, section: TemplateRepository.CategorySection) {
        // Track category được mở (See All / tên category) — bỏ emoji, chỉ giữ chữ+số+space cho gọn.
        val cat = section.title.filter { it.isLetterOrDigit() || it == ' ' }.trim()
        WeatherApplication.trackingEvent("select_category", "category", cat)
        val open = { CategoryTemplatesActivity.start(ctx, section.title, section.templates) }
        if (ctx is Activity) InterAds.showAdsBreak(ctx) { open() } else open()
    }

    override fun getItemCount() = sections.size
}
