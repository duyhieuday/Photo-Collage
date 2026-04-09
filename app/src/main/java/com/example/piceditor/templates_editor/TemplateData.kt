package com.example.piceditor.templates_editor

import android.graphics.RectF

data class TemplateData(
    val id: String,
    val name: String,
    val drawableRes: Int,       // Ảnh template gốc (1125x2000) để edit
    val thumbRes: Int,          // Ảnh thumbnail nhỏ để hiện ở RecyclerView
    val cellRects: List<RectF>, // Tọa độ ô ảnh theo không gian gốc 1125x2000
    val maskMode: MaskMode = MaskMode.WHITE
)

enum class MaskMode {
    WHITE,  // Ô ảnh là vùng trắng
    BLACK   // Ô ảnh là vùng đen
}

object TemplateRepository {

    val all: List<TemplateData> by lazy {
        listOf(
            // ── Temp 1: Best Trip ──────────────────────────────
            // thumb_1  →  temp_1
            TemplateData(
                id = "temp_1",
                name = "Best Trip",
                drawableRes = com.example.piceditor.R.drawable.temp_1,
                thumbRes    = com.example.piceditor.R.drawable.thumb_1,
                cellRects = listOf(
                    RectF(124f, 179f,  536f,  592f),
                    RectF(102f, 616f,  515f,  1029f),
                    RectF(79f,  1053f, 492f,  1466f),
                    RectF(602f, 755f,  1011f, 1164f),
                    RectF(619f, 1192f, 1029f, 1602f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 2: Life is Beautiful ──────────────────────
            // thumb_2  →  temp_2
            TemplateData(
                id = "temp_2",
                name = "Life is Beautiful",
                drawableRes = com.example.piceditor.R.drawable.temp_2,
                thumbRes    = com.example.piceditor.R.drawable.thumb_2,
                cellRects = listOf(
                    RectF(145f, 344f,  560f,  759f),
                    RectF(602f, 344f,  1017f, 759f),
                    RectF(144f, 802f,  559f,  1217f),
                    RectF(604f, 802f,  1018f, 1217f),
                    RectF(145f, 1261f, 560f,  1676f),
                    RectF(602f, 1261f, 1017f, 1676f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 5: Vinyl Memories ─────────────────────────
            // thumb_3  →  temp_5
            TemplateData(
                id = "temp_5",
                name = "Vinyl Memories",
                drawableRes = com.example.piceditor.R.drawable.temp_5,
                thumbRes    = com.example.piceditor.R.drawable.thumb_3,
                cellRects = listOf(
                    RectF(509f, 181f, 1051f, 736f),
                    RectF(104f, 974f, 663f,  1539f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 6: Happy Day Blue ─────────────────────────
            // thumb_4  →  temp_6
            TemplateData(
                id = "temp_6",
                name = "Happy Day",
                drawableRes = com.example.piceditor.R.drawable.temp_6,
                thumbRes    = com.example.piceditor.R.drawable.thumb_4,
                cellRects = listOf(
                    RectF(30f,  168f, 490f,  640f),
                    RectF(415f, 330f, 1080f, 775f),
                    RectF(30f,  770f, 490f,  1290f),
                    RectF(415f, 790f, 1080f, 1300f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 7: Music Player Daisy ─────────────────────
            // thumb_5  →  temp_7
            TemplateData(
                id = "temp_7",
                name = "Music Vibes",
                drawableRes = com.example.piceditor.R.drawable.temp_7,
                thumbRes    = com.example.piceditor.R.drawable.thumb_5,
                cellRects = listOf(
                    RectF(132f, 95f, 960f, 720f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 8: My Life My Way ─────────────────────────
            // thumb_6  →  temp_8
            TemplateData(
                id = "temp_8",
                name = "My Life My Way",
                drawableRes = com.example.piceditor.R.drawable.temp_8,
                thumbRes    = com.example.piceditor.R.drawable.thumb_6,
                cellRects = listOf(
                    RectF(145f, 310f, 975f, 960f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 9: Hello Summer ───────────────────────────
            // thumb_7  →  temp_9
            TemplateData(
                id = "temp_9",
                name = "Hello Summer",
                drawableRes = com.example.piceditor.R.drawable.temp_9,
                thumbRes    = com.example.piceditor.R.drawable.thumb_7,
                cellRects = listOf(
                    RectF(195f, 250f,  960f, 990f),
                    RectF(494f, 560f,  762f, 790f),
                    RectF(487f, 810f,  762f, 1060f),
                    RectF(487f, 1070f, 762f, 1330f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 10: Dear My Darling ───────────────────────
            // thumb_8  →  temp_10
            TemplateData(
                id = "temp_10",
                name = "Dear My Darling",
                drawableRes = com.example.piceditor.R.drawable.temp_10,
                thumbRes    = com.example.piceditor.R.drawable.thumb_8,
                cellRects = listOf(
                    RectF(295f, 370f, 880f,  870f),
                    RectF(180f, 795f, 760f, 1345f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 3 (Snoopy Birthday) ───────────────────────
            // thumb_9  →  temp_3 (nếu có file temp_3)
            // Uncomment khi bạn thêm file temp_3.png vào drawable
            /*
            TemplateData(
                id = "temp_3",
                name = "Happy Birthday",
                drawableRes = com.example.piceditor.R.drawable.temp_3,
                thumbRes    = com.example.piceditor.R.drawable.thumb_9,
                cellRects = listOf(
                    RectF(230f, 330f, 780f,  750f),
                    RectF(48f,  720f, 610f,  1240f),
                    RectF(420f, 990f, 975f,  1460f)
                ),
                maskMode = MaskMode.WHITE
            ),
            */

            // ── Temp 4 (Lovers Club) ───────────────────────────
            // thumb_10  →  temp_4 (nếu có file temp_4)
            // Uncomment khi bạn thêm file temp_4.png vào drawable
            /*
            TemplateData(
                id = "temp_4",
                name = "Lovers Club",
                drawableRes = com.example.piceditor.R.drawable.temp_4,
                thumbRes    = com.example.piceditor.R.drawable.thumb_10,
                cellRects = listOf(
                    RectF(298f, 580f, 946f,  1143f),
                    RectF(279f, 1145f, 659f, 1576f)
                ),
                maskMode = MaskMode.WHITE
            ),
            */
        )
    }

    fun findById(id: String) = all.find { it.id == id }
}
