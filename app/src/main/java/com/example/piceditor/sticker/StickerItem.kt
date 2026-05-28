package com.example.piceditor.sticker

data class StickerItem(val assetPath: String)

data class StickerCategory(
    val id: String,
    val preview: StickerItem,
    val items: List<StickerItem>
)
