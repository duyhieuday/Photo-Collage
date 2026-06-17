package com.example.piceditor.templates_editor

import android.graphics.RectF
import com.example.piceditor.R

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
    BLACK,

    // Mask theo ô XÁM #ededed: pixel xám trung tính -> trong suốt -> ảnh hiện ĐÚNG
    // theo hình khung xám (kể cả nghiêng/bo góc), ô chỉ cần bao trùm khung.
    GRAY,

    // Như GRAY nhưng cho ô xám SÁNG/nhạt hơn (~#d9d9d9, R/G/B ~208..228).
    // Dùng cho template có ô xám ngoài dải GRAY (vd sm02 ~217, sm04 ~222).
    GRAY2,

    // Không tạo mask overlay: ảnh fill thẳng theo cellRect.
    NONE
}

// Bộ template import từ folder Category_Template (ảnh JPEG đã downscale 1125x2000).
// cellRects + maskMode lấy từ TemplateCells (auto-generate từ tools/cells_config.ps1).
object TemplateRepository {

    private fun cells(id: String): List<RectF> = TemplateCells.rects[id] ?: emptyList()
    private fun mask(id: String): MaskMode = TemplateCells.masks[id] ?: MaskMode.NONE

    val all: List<TemplateData> by lazy {
        listOf(
            // ── Birthday ──────────────────────────────────────
            TemplateData("bd01", "BD01", R.drawable.temp_bd01, R.drawable.thumb_bd01, cells("bd01"), mask("bd01")),
            TemplateData("bd02", "BD02", R.drawable.temp_bd02, R.drawable.thumb_bd02, cells("bd02"), mask("bd02")),
            TemplateData("bd03", "BD03", R.drawable.temp_bd03, R.drawable.thumb_bd03, cells("bd03"), mask("bd03")),
            TemplateData("bd04", "BD04", R.drawable.temp_bd04, R.drawable.thumb_bd04, cells("bd04"), mask("bd04")),
            TemplateData("bd05", "BD05", R.drawable.temp_bd05, R.drawable.thumb_bd05, cells("bd05"), mask("bd05")),
            TemplateData("bd06", "BD06", R.drawable.temp_bd06, R.drawable.thumb_bd06, cells("bd06"), mask("bd06")),
            TemplateData("bd07", "BD07", R.drawable.temp_bd07, R.drawable.thumb_bd07, cells("bd07"), mask("bd07")),
            TemplateData("bd08", "BD08", R.drawable.temp_bd08, R.drawable.thumb_bd08, cells("bd08"), mask("bd08")),
            TemplateData("bd09", "BD09", R.drawable.temp_bd09, R.drawable.thumb_bd09, cells("bd09"), mask("bd09")),
            TemplateData("bd10", "BD10", R.drawable.temp_bd10, R.drawable.thumb_bd10, cells("bd10"), mask("bd10")),

            // ── Couple ────────────────────────────────────────
            TemplateData("cp01", "CP01", R.drawable.temp_cp01, R.drawable.thumb_cp01, cells("cp01"), mask("cp01")),
            TemplateData("cp02", "CP02", R.drawable.temp_cp02, R.drawable.thumb_cp02, cells("cp02"), mask("cp02")),
            TemplateData("cp03", "CP03", R.drawable.temp_cp03, R.drawable.thumb_cp03, cells("cp03"), mask("cp03")),
            TemplateData("cp04", "CP04", R.drawable.temp_cp04, R.drawable.thumb_cp04, cells("cp04"), mask("cp04")),
            TemplateData("cp05", "CP05", R.drawable.temp_cp05, R.drawable.thumb_cp05, cells("cp05"), mask("cp05")),
            TemplateData("cp06", "CP06", R.drawable.temp_cp06, R.drawable.thumb_cp06, cells("cp06"), mask("cp06")),
            TemplateData("cp07", "CP07", R.drawable.temp_cp07, R.drawable.thumb_cp07, cells("cp07"), mask("cp07")),
            TemplateData("cp08", "CP08", R.drawable.temp_cp08, R.drawable.thumb_cp08, cells("cp08"), mask("cp08")),
            TemplateData("cp09", "CP09", R.drawable.temp_cp09, R.drawable.thumb_cp09, cells("cp09"), mask("cp09")),

            // ── Glad season ───────────────────────────────────
            TemplateData("gs01", "GS01", R.drawable.temp_gs01, R.drawable.thumb_gs01, cells("gs01"), mask("gs01")),
            TemplateData("gs02", "GS02", R.drawable.temp_gs02, R.drawable.thumb_gs02, cells("gs02"), mask("gs02")),
            TemplateData("gs03", "GS03", R.drawable.temp_gs03, R.drawable.thumb_gs03, cells("gs03"), mask("gs03")),
            TemplateData("gs04", "GS04", R.drawable.temp_gs04, R.drawable.thumb_gs04, cells("gs04"), mask("gs04")),
            TemplateData("gs05", "GS05", R.drawable.temp_gs05, R.drawable.thumb_gs05, cells("gs05"), mask("gs05")),
            TemplateData("gs06", "GS06", R.drawable.temp_gs06, R.drawable.thumb_gs06, cells("gs06"), mask("gs06")),
            TemplateData("gs07", "GS07", R.drawable.temp_gs07, R.drawable.thumb_gs07, cells("gs07"), mask("gs07")),
            TemplateData("gs08", "GS08", R.drawable.temp_gs08, R.drawable.thumb_gs08, cells("gs08"), mask("gs08")),
            TemplateData("gs09", "GS09", R.drawable.temp_gs09, R.drawable.thumb_gs09, cells("gs09"), mask("gs09")),
            TemplateData("gs10", "GS10", R.drawable.temp_gs10, R.drawable.thumb_gs10, cells("gs10"), mask("gs10")),

            // ── IG Story ──────────────────────────────────────
            TemplateData("is01", "IS01", R.drawable.temp_is01, R.drawable.thumb_is01, cells("is01"), mask("is01")),
            TemplateData("is02", "IS02", R.drawable.temp_is02, R.drawable.thumb_is02, cells("is02"), mask("is02")),
            TemplateData("is03", "IS03", R.drawable.temp_is03, R.drawable.thumb_is03, cells("is03"), mask("is03")),
            TemplateData("is04", "IS04", R.drawable.temp_is04, R.drawable.thumb_is04, cells("is04"), mask("is04")),
            TemplateData("is05", "IS05", R.drawable.temp_is05, R.drawable.thumb_is05, cells("is05"), mask("is05")),
            TemplateData("is06", "IS06", R.drawable.temp_is06, R.drawable.thumb_is06, cells("is06"), mask("is06")),
            TemplateData("is07", "IS07", R.drawable.temp_is07, R.drawable.thumb_is07, cells("is07"), mask("is07")),
            TemplateData("is08", "IS08", R.drawable.temp_is08, R.drawable.thumb_is08, cells("is08"), mask("is08")),
            TemplateData("is09", "IS09", R.drawable.temp_is09, R.drawable.thumb_is09, cells("is09"), mask("is09")),
            TemplateData("is10", "IS10", R.drawable.temp_is10, R.drawable.thumb_is10, cells("is10"), mask("is10")),
            TemplateData("is11", "IS11", R.drawable.temp_is11, R.drawable.thumb_is11, cells("is11"), mask("is11")),
            TemplateData("is12", "IS12", R.drawable.temp_is12, R.drawable.thumb_is12, cells("is12"), mask("is12")),
            TemplateData("is13", "IS13", R.drawable.temp_is13, R.drawable.thumb_is13, cells("is13"), mask("is13")),
            TemplateData("is14", "IS14", R.drawable.temp_is14, R.drawable.thumb_is14, cells("is14"), mask("is14")),
            TemplateData("is15", "IS15", R.drawable.temp_is15, R.drawable.thumb_is15, cells("is15"), mask("is15")),

            // ── Summer vibe ───────────────────────────────────
            TemplateData("sm01", "SM01", R.drawable.temp_sm01, R.drawable.thumb_sm01, cells("sm01"), mask("sm01")),
            TemplateData("sm02", "SM02", R.drawable.temp_sm02, R.drawable.thumb_sm02, cells("sm02"), mask("sm02")),
            TemplateData("sm03", "SM03", R.drawable.temp_sm03, R.drawable.thumb_sm03, cells("sm03"), mask("sm03")),
            TemplateData("sm04", "SM04", R.drawable.temp_sm04, R.drawable.thumb_sm04, cells("sm04"), mask("sm04")),
            TemplateData("sm05", "SM05", R.drawable.temp_sm05, R.drawable.thumb_sm05, cells("sm05"), mask("sm05")),
            TemplateData("sm06", "SM06", R.drawable.temp_sm06, R.drawable.thumb_sm06, cells("sm06"), mask("sm06")),
            TemplateData("sm07", "SM07", R.drawable.temp_sm07, R.drawable.thumb_sm07, cells("sm07"), mask("sm07")),
            TemplateData("sm08", "SM08", R.drawable.temp_sm08, R.drawable.thumb_sm08, cells("sm08"), mask("sm08")),
            TemplateData("sm09", "SM09", R.drawable.temp_sm09, R.drawable.thumb_sm09, cells("sm09"), mask("sm09")),

            // ── Sports (SP01 từ folder Popular vì folder Sports rỗng) ──
            TemplateData("sp01", "SP01", R.drawable.temp_sp01, R.drawable.thumb_sp01, cells("sp01"), mask("sp01"))
        )
    }

    fun findById(id: String) = all.find { it.id == id }

    // ── Category cho màn picker (đúng thứ tự Figma) ──────────────────────
    enum class TemplateCategory(val title: String) {
        SUMMER("☀️ Summer vibe"),
        IG_STORY("📸 IG Story"),
        BIRTHDAY("🎂 Birthday"),
        COUPLE("💞 Couple"),
        GLAD_SEASON("🍂 Glad season"),
        SPORTS("⚽ Sports")
    }

    data class CategorySection(
        val title: String,
        val templates: List<TemplateData>
    )

    private fun categoryOf(id: String): TemplateCategory = when {
        id.startsWith("bd") -> TemplateCategory.BIRTHDAY
        id.startsWith("cp") -> TemplateCategory.COUPLE
        id.startsWith("gs") -> TemplateCategory.GLAD_SEASON
        id.startsWith("is") -> TemplateCategory.IG_STORY
        id.startsWith("sm") -> TemplateCategory.SUMMER
        else                -> TemplateCategory.SPORTS
    }

    // Hàng "🔥 Most popular" = bộ curated trong folder Popular.
    private val popularIds = listOf("cp03", "cp06", "gs03", "is05", "is06", "is12", "sm05", "sp01")

    val sections: List<CategorySection> by lazy {
        val byId = all.associateBy { it.id }
        buildList {
            val popular = popularIds.mapNotNull { byId[it] }
            if (popular.isNotEmpty()) add(CategorySection("🔥 Most popular", popular))
            for (cat in TemplateCategory.values()) {
                val items = all.filter { categoryOf(it.id) == cat }
                if (items.isNotEmpty()) add(CategorySection(cat.title, items))
            }
        }
    }
}
