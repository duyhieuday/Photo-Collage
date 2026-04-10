package com.example.piceditor.templates_editor

import android.graphics.RectF

data class TemplateData(
    val id: String,
    val name: String,
    val drawableRes: Int,
    val thumbRes: Int,
    val cellRects: List<RectF>,
    val maskMode: MaskMode = MaskMode.WHITE
)

enum class MaskMode {
    WHITE,  // Ô ảnh là vùng trắng
    BLACK   // Ô ảnh là vùng đen
}

// Tọa độ cellRects tính trên không gian 1125 x 2000
object TemplateRepository {

    val all: List<TemplateData> by lazy {
        listOf(

            // ── Temp 1: Best Trip ─────────────────────────────
            // thumb_1 → temp_1
            // 5 ô: 3 cột trái + 2 cột phải
            TemplateData(
                id          = "temp_1",
                name        = "Best Trip",
                drawableRes = com.example.piceditor.R.drawable.temp_1,
                thumbRes    = com.example.piceditor.R.drawable.thumb_1,
                cellRects   = listOf(
                    RectF(124f, 179f,  537f,  592f),
                    RectF(101f, 616f,  515f,  1029f),
                    RectF(79f,  1053f, 492f,  1466f),
                    RectF(602f, 755f,  1011f, 1164f),
                    RectF(619f, 1192f, 1028f, 1602f)
                ),
                maskMode    = MaskMode.WHITE
            ),

            // ── Temp 2: Life is Beautiful ─────────────────────
            // thumb_2 → temp_2
            // 6 ô lưới 2x3, nền đen
            TemplateData(
                id          = "temp_2",
                name        = "Life is Beautiful",
                drawableRes = com.example.piceditor.R.drawable.temp_2,
                thumbRes    = com.example.piceditor.R.drawable.thumb_2,
                cellRects   = listOf(
                    RectF(145f, 344f,  560f,  759f),
                    RectF(603f, 344f,  1017f, 759f),
                    RectF(144f, 803f,  559f,  1217f),
                    RectF(604f, 803f,  1018f, 1217f),
                    RectF(145f, 1261f, 560f,  1676f),
                    RectF(603f, 1261f, 1017f, 1676f)
                ),
                maskMode    = MaskMode.WHITE
            ),

            // ── Temp 3: Happy Birthday Snoopy ─────────────────
            // thumb_3 → temp_3
            // 3 ô polaroid xếp chéo
            TemplateData(
                id          = "temp_3",
                name        = "Happy Birthday",
                drawableRes = com.example.piceditor.R.drawable.temp_3,
                thumbRes    = com.example.piceditor.R.drawable.thumb_3,
                cellRects   = listOf(
                    RectF(348f, 421f,  876f,  964f),
                    RectF(93f,  988f,  583f,  1503f),
                    RectF(531f, 1355f, 1109f, 1954f)
                ),
                maskMode    = MaskMode.WHITE
            ),

            // ── Temp 4: Lovers Club ───────────────────────────
            // thumb_4 → temp_4
            // 2 ô: màn hình máy ảnh + khung Lovers Club
            TemplateData(
                id          = "temp_4",
                name        = "Lovers Club",
                drawableRes = com.example.piceditor.R.drawable.temp_4,
                thumbRes    = com.example.piceditor.R.drawable.thumb_4,
                cellRects   = listOf(
                    RectF(449f, 484f,  828f,  966f),
                    RectF(181f, 1281f, 560f,  1661f)
                ),
                maskMode    = MaskMode.WHITE
            ),

            // ── Temp 5: Vinyl Memories ────────────────────────
            // thumb_5 → temp_5
            // 2 ô polaroid trên đĩa vinyl
            TemplateData(
                id          = "temp_5",
                name        = "Vinyl Memories",
                drawableRes = com.example.piceditor.R.drawable.temp_5,
                thumbRes    = com.example.piceditor.R.drawable.thumb_5,
                cellRects   = listOf(
                    RectF(510f, 181f, 1051f, 734f),
                    RectF(105f, 974f, 662f,  1538f)
                ),
                maskMode    = MaskMode.WHITE
            ),

            // ── Temp 6: Happy Day ─────────────────────────────
            // thumb_6 → temp_6
            // 4 ô scrapbook xanh
            TemplateData(
                id          = "temp_6",
                name        = "Happy Day",
                drawableRes = com.example.piceditor.R.drawable.temp_6,
                thumbRes    = com.example.piceditor.R.drawable.thumb_6,
                cellRects   = listOf(
                    RectF(66f,  275f,  541f,  899f),
                    RectF(578f, 399f,  1072f, 1023f),
                    RectF(52f,  923f,  543f,  1607f),
                    RectF(583f, 1097f, 1059f, 1721f)
                ),
                maskMode    = MaskMode.WHITE
            ),

            // ── Temp 7: Music Vibes (Daisy) ───────────────────
            // thumb_7 → temp_7
            // 1 ô lớn kiểu music player
            TemplateData(
                id          = "temp_7",
                name        = "Music Vibes",
                drawableRes = com.example.piceditor.R.drawable.temp_7,
                thumbRes    = com.example.piceditor.R.drawable.thumb_7,
                cellRects   = listOf(
                    RectF(155f, 212f, 971f, 1023f)
                ),
                maskMode    = MaskMode.WHITE
            ),

            // ── Temp 8: My Life My Way ────────────────────────
            // thumb_8 → temp_8
            // 1 ô vuông lớn, nền xanh navy
            TemplateData(
                id          = "temp_8",
                name        = "My Life My Way",
                drawableRes = com.example.piceditor.R.drawable.temp_8,
                thumbRes    = com.example.piceditor.R.drawable.thumb_8,
                cellRects   = listOf(
                    RectF(251f, 599f, 900f, 1286f)
                ),
                maskMode    = MaskMode.WHITE
            ),

            // ── Temp 9: Hello Summer ──────────────────────────
            // thumb_9 → temp_9
            // 1 ô Instagram lớn + 3 ô polaroid nhỏ
            TemplateData(
                id          = "temp_9",
                name        = "Hello Summer",
                drawableRes = com.example.piceditor.R.drawable.temp_9,
                thumbRes    = com.example.piceditor.R.drawable.thumb_9,
                cellRects   = listOf(
                    RectF(196f, 250f,  893f, 705f),   // Instagram post
                    RectF(494f, 565f,  753f, 795f),   // Polaroid 1
                    RectF(487f, 810f,  753f, 1060f),  // Polaroid 2
                    RectF(487f, 1075f, 753f, 1330f)   // Polaroid 3
                ),
                maskMode    = MaskMode.WHITE
            ),

            // ── Temp 10: Dear My Darling ──────────────────────
            // thumb_10 → temp_10
            // 2 ô tờ giấy trắng trong sổ tay
            TemplateData(
                id          = "temp_10",
                name        = "Dear My Darling",
                drawableRes = com.example.piceditor.R.drawable.temp_10,
                thumbRes    = com.example.piceditor.R.drawable.thumb_10,
                cellRects   = listOf(
                    RectF(298f, 581f,  947f,  1143f),
                    RectF(279f, 1145f, 659f,  1576f)
                ),
                maskMode    = MaskMode.WHITE
            )
        )
    }

    fun findById(id: String) = all.find { it.id == id }
}