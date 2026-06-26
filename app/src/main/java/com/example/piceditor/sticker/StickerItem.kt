package com.example.piceditor.sticker

data class StickerItem(val assetPath: String, val isPremium: Boolean = false)

data class StickerCategory(
    val id: String,
    val preview: StickerItem,
    val items: List<StickerItem>
)
