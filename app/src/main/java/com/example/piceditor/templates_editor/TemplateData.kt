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
    WHITE,
    BLACK
}

// Tọa độ cellRects tính trên không gian 1125 x 2000
// Đo từ ảnh gốc 3240x5760, scale=0.8391, dx=0, dy=0.39
// Mỗi cell mở rộng thêm 8px mỗi chiều so với detect gốc
object TemplateRepository {

    val all: List<TemplateData> by lazy {
        listOf(

            // ── Temp 1: Best Trip ─────────────────────────────
            TemplateData(
                id          = "1",
                name        = "Best Trip",
                drawableRes = com.example.piceditor.R.drawable.temp_1,
                thumbRes    = com.example.piceditor.R.drawable.thumb_1,
                cellRects   = listOf(
                    RectF(118f, 174f, 542f,  597f),
                    RectF(96f,  611f, 520f,  1034f),
                    RectF(74f,  1048f, 498f, 1471f),
                    RectF(596f, 750f,  1016f, 1169f),
                    RectF(614f, 1187f, 1033f, 1607f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 2: Life is Beautiful ─────────────────────
            TemplateData(
                id          = "2",
                name        = "Life is Beautiful",
                drawableRes = com.example.piceditor.R.drawable.temp_2,
                thumbRes    = com.example.piceditor.R.drawable.thumb_2,
                cellRects   = listOf(
                    RectF(140f, 339f, 565f,  764f),
                    RectF(597f, 339f, 1022f, 764f),
                    RectF(139f, 798f, 564f,  1222f),
                    RectF(599f, 798f, 1023f, 1222f),
                    RectF(140f, 1256f, 565f, 1681f),
                    RectF(597f, 1256f, 1022f, 1681f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 3: Happy Birthday Snoopy ─────────────────
            TemplateData(
                id          = "3",
                name        = "Happy Birthday",
                drawableRes = com.example.piceditor.R.drawable.temp_3,
                thumbRes    = com.example.piceditor.R.drawable.thumb_3,
                cellRects   = listOf(
                    RectF(342f, 416f, 881f,  972f),
                    RectF(87f,  983f, 588f,  1508f),
                    RectF(526f, 1350f, 1114f, 1959f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 4: Lovers Club ───────────────────────────
            TemplateData(
                id          = "4",
                name        = "Lovers Club",
                drawableRes = com.example.piceditor.R.drawable.temp_4,
                thumbRes    = com.example.piceditor.R.drawable.thumb_4,
                cellRects   = listOf(
                    RectF(444f, 479f, 833f, 971f),
                    RectF(175f, 1276f, 565f, 1667f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 5: Vinyl Memories ────────────────────────
            TemplateData(
                id          = "5",
                name        = "Vinyl Memories",
                drawableRes = com.example.piceditor.R.drawable.temp_5,
                thumbRes    = com.example.piceditor.R.drawable.thumb_5,
                cellRects   = listOf(
                    RectF(502f, 176f, 1056f, 741f),
                    RectF(100f, 969f, 668f,  1543f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 6: Happy Day ─────────────────────────────
            TemplateData(
                id          = "6",
                name        = "Happy Day",
                drawableRes = com.example.piceditor.R.drawable.temp_6,
                thumbRes    = com.example.piceditor.R.drawable.thumb_6,
                cellRects   = listOf(
                    RectF(61f,  270f,  546f,  904f),
                    RectF(543f, 394f,  1077f, 1040f),
                    RectF(47f,  906f,  588f,  1612f),
                    RectF(578f, 1092f, 1064f, 1726f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 7: Music Vibes ───────────────────────────
            TemplateData(
                id          = "7",
                name        = "Music Vibes",
                drawableRes = com.example.piceditor.R.drawable.temp_7,
                thumbRes    = com.example.piceditor.R.drawable.thumb_7,
                cellRects   = listOf(
                    RectF(149f, 207f, 976f, 1040f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 8: My Life My Way ────────────────────────
            TemplateData(
                id          = "8",
                name        = "My Life My Way",
                drawableRes = com.example.piceditor.R.drawable.temp_8,
                thumbRes    = com.example.piceditor.R.drawable.thumb_8,
                cellRects   = listOf(
                    RectF(246f, 594f, 905f, 1291f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 10: Dear My Darling ──────────────────────
            TemplateData(
                id          = "10",
                name        = "Dear My Darling",
                drawableRes = com.example.piceditor.R.drawable.temp_10,
                thumbRes    = com.example.piceditor.R.drawable.thumb_10,
                cellRects   = listOf(
                    RectF(293f, 576f, 952f, 1148f),
                    RectF(274f, 1140f, 664f, 1581f)
                ),
                maskMode = MaskMode.WHITE
            )
        )
    }

    fun findById(id: String) = all.find { it.id == id }
}