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
                    RectF(123f, 179f,  538f,  594f),
                    RectF(101f, 616f,  516f,  1031f),
                    RectF(79f,  1053f, 494f,  1468f),
                    RectF(601f, 755f,  1012f, 1166f),
                    RectF(619f, 1192f, 1030f, 1603f)
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
                    RectF(145f, 344f,  561f,  760f),
                    RectF(602f, 344f,  1019f, 760f),
                    RectF(144f, 802f,  560f,  1219f),
                    RectF(603f, 802f,  1020f, 1219f),
                    RectF(145f, 1260f, 561f,  1677f),
                    RectF(602f, 1260f, 1019f, 1677f)
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
                    RectF(449f, 484f, 829f, 967f),
                    RectF(180f, 1281f, 561f, 1662f)
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
                    RectF(508f, 181f, 1052f, 735f),
                    RectF(105f, 974f, 664f,  1540f)
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
                    RectF(154f, 211f, 972f, 1024f)
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
                    RectF(251f, 599f, 901f, 1288f)
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
                    RectF(297f, 579f, 948f, 1145f),
                    RectF(278f, 1145f, 661f, 1577f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 11: Mood Today ───────────────────────────
            TemplateData(
                id          = "11",
                name        = "Mood Today",
                drawableRes = com.example.piceditor.R.drawable.temp_11,
                thumbRes    = com.example.piceditor.R.drawable.thumb_11,
                cellRects   = listOf(
                    RectF(44f,  261f,  623f,  724f),
                    RectF(628f, 261f,  1081f, 892f),
                    RectF(44f,  729f,  498f,  1359f),
                    RectF(503f, 896f,  1081f, 1359f),
                    RectF(44f,  1364f, 1081f, 1828f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 12: Sunday Picnic ────────────────────────
            TemplateData(
                id          = "12",
                name        = "Sunday Picnic",
                drawableRes = com.example.piceditor.R.drawable.temp_12,
                thumbRes    = com.example.piceditor.R.drawable.thumb_12,
                cellRects   = listOf(
                    RectF(45f, 120f,  1083f, 704f),
                    RectF(71f, 761f,  1059f, 1260f),
                    RectF(46f, 1319f, 1083f, 1906f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 13: Save The Moments ─────────────────────
            TemplateData(
                id          = "13",
                name        = "Save The Moments",
                drawableRes = com.example.piceditor.R.drawable.temp_13,
                thumbRes    = com.example.piceditor.R.drawable.thumb_13,
                cellRects   = listOf(
                    RectF(110f, 117f,  764f,  751f),
                    RectF(499f, 714f,  1076f, 1292f),
                    RectF(54f,  1186f, 707f,  1830f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 15: Happy Anniversary ────────────────────
            TemplateData(
                id          = "15",
                name        = "Happy Anniversary",
                drawableRes = com.example.piceditor.R.drawable.temp_15,
                thumbRes    = com.example.piceditor.R.drawable.thumb_15,
                cellRects   = listOf(
                    RectF(218f, 259f, 1014f, 777f),
                    RectF(164f, 773f, 905f,  1295f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 16: Enjoy Life ───────────────────────────
            TemplateData(
                id          = "16",
                name        = "Enjoy Life",
                drawableRes = com.example.piceditor.R.drawable.temp_16,
                thumbRes    = com.example.piceditor.R.drawable.thumb_16,
                cellRects   = listOf(
                    RectF(131f, 547f,  393f, 857f),
                    RectF(424f, 546f,  996f, 1543f),
                    RectF(131f, 888f,  393f, 1201f),
                    RectF(131f, 1231f, 393f, 1543f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 17: Graduation 2026 ──────────────────────
            TemplateData(
                id          = "17",
                name        = "Graduation 2026",
                drawableRes = com.example.piceditor.R.drawable.temp_17,
                thumbRes    = com.example.piceditor.R.drawable.thumb_17,
                cellRects   = listOf(
                    RectF(187f, 349f, 938f, 839f),
                    RectF(187f, 870f, 938f, 1287f)
                ),
                maskMode = MaskMode.WHITE
            ),

            // ── Temp 18: Torn Paper ───────────────────────────
            TemplateData(
                id          = "18",
                name        = "Torn Paper",
                drawableRes = com.example.piceditor.R.drawable.temp_18,
                thumbRes    = com.example.piceditor.R.drawable.thumb_18,
                cellRects   = listOf(
                    RectF(0f, 0f,    1125f, 664f),
                    RectF(0f, 653f,  1125f, 1342f),
                    RectF(0f, 1322f, 1125f, 2000f)
                ),
                maskMode = MaskMode.BLACK
            ),

            // ── Temp 19: Puzzle Pieces ────────────────────────
            TemplateData(
                id          = "19",
                name        = "Puzzle Pieces",
                drawableRes = com.example.piceditor.R.drawable.temp_19,
                thumbRes    = com.example.piceditor.R.drawable.thumb_19,
                cellRects   = listOf(
                    RectF(0f,   0f,    566f,  665f),
                    RectF(522f, 0f,    1125f, 715f),
                    RectF(0f,   617f,  603f,  1335f),
                    RectF(560f, 666f,  1125f, 1379f),
                    RectF(0f,   1283f, 566f,  2000f),
                    RectF(506f, 1339f, 1125f, 2000f)
                ),
                maskMode = MaskMode.BLACK
            ),

            // ── Temp 20: You Can Do It ────────────────────────
            TemplateData(
                id          = "20",
                name        = "You Can Do It",
                drawableRes = com.example.piceditor.R.drawable.temp_20,
                thumbRes    = com.example.piceditor.R.drawable.thumb_20,
                cellRects   = listOf(
                    RectF(13f,  143f,  376f,  628f),
                    RectF(381f, 279f,  744f,  764f),
                    RectF(749f, 392f,  1113f, 877f),
                    RectF(13f,  633f,  376f,  1118f),
                    RectF(381f, 768f,  744f,  1253f),
                    RectF(749f, 882f,  1113f, 1367f),
                    RectF(13f,  1122f, 376f,  1612f),
                    RectF(380f, 1258f, 744f,  1743f),
                    RectF(749f, 1371f, 1113f, 1856f)
                ),
                maskMode = MaskMode.WHITE
            )
        )
    }

    fun findById(id: String) = all.find { it.id == id }
}