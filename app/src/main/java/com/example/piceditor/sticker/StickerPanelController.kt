package com.example.piceditor.sticker

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piceditor.draw.model.sticker.StickerData

class StickerPanelController(
    private val context: Context,
    private val categoryRecycler: RecyclerView,
    private val gridRecycler: RecyclerView,
    private val onAddSticker: (StickerData) -> Unit
) {

    private val categories: List<StickerCategory> = StickerCatalog.build(context)
    private val gridAdapter = StickerGridAdapter(::handlePick)
    private val categoryAdapter = StickerCategoryAdapter(categories) { _, cat ->
        gridAdapter.submit(cat.items)
    }

    init {
        categoryRecycler.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        categoryRecycler.adapter = categoryAdapter

        gridRecycler.layoutManager = GridLayoutManager(context, 7)
        gridRecycler.adapter = gridAdapter

        if (categories.isNotEmpty()) {
            gridAdapter.submit(categories.first().items)
        }
    }

    private fun handlePick(item: StickerItem) {
        onAddSticker(StickerData(item.assetPath))
    }
}
