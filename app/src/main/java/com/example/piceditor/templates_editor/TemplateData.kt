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
) {
    // Cách A: template có số thứ tự >= 6 trong category là PREMIUM (giữ ~5 đầu/category free)
    val isPremium: Boolean
        get() = (id.filter { it.isDigit() }.toIntOrNull() ?: 0) >= 6
}

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
            TemplateData("bd11", "BD11", R.drawable.temp_bd11, R.drawable.thumb_bd11, cells("bd11"), mask("bd11")),
            TemplateData("bd12", "BD12", R.drawable.temp_bd12, R.drawable.thumb_bd12, cells("bd12"), mask("bd12")),
            TemplateData("bd13", "BD13", R.drawable.temp_bd13, R.drawable.thumb_bd13, cells("bd13"), mask("bd13")),
            TemplateData("bd14", "BD14", R.drawable.temp_bd14, R.drawable.thumb_bd14, cells("bd14"), mask("bd14")),
            TemplateData("bd15", "BD15", R.drawable.temp_bd15, R.drawable.thumb_bd15, cells("bd15"), mask("bd15")),
            TemplateData("bd16", "BD16", R.drawable.temp_bd16, R.drawable.thumb_bd16, cells("bd16"), mask("bd16")),
            TemplateData("bd17", "BD17", R.drawable.temp_bd17, R.drawable.thumb_bd17, cells("bd17"), mask("bd17")),
            TemplateData("bd18", "BD18", R.drawable.temp_bd18, R.drawable.thumb_bd18, cells("bd18"), mask("bd18")),
            TemplateData("bd19", "BD19", R.drawable.temp_bd19, R.drawable.thumb_bd19, cells("bd19"), mask("bd19")),
            TemplateData("bd20", "BD20", R.drawable.temp_bd20, R.drawable.thumb_bd20, cells("bd20"), mask("bd20")),

            // ── Couple ────────────────────────────────────────
            // cp01 mở lại ở đợt import 2: cellRects cũ làm ô lệch khung nên từng bị ẩn,
            // nay đo lại bằng diff Temp_/Thumb_ và đã verify ô khớp ảnh.
            TemplateData("cp01", "CP01", R.drawable.temp_cp01, R.drawable.thumb_cp01, cells("cp01"), mask("cp01")),
            TemplateData("cp02", "CP02", R.drawable.temp_cp02, R.drawable.thumb_cp02, cells("cp02"), mask("cp02")),
            TemplateData("cp03", "CP03", R.drawable.temp_cp03, R.drawable.thumb_cp03, cells("cp03"), mask("cp03")),
            TemplateData("cp04", "CP04", R.drawable.temp_cp04, R.drawable.thumb_cp04, cells("cp04"), mask("cp04")),
            TemplateData("cp05", "CP05", R.drawable.temp_cp05, R.drawable.thumb_cp05, cells("cp05"), mask("cp05")),
            TemplateData("cp06", "CP06", R.drawable.temp_cp06, R.drawable.thumb_cp06, cells("cp06"), mask("cp06")),
            TemplateData("cp07", "CP07", R.drawable.temp_cp07, R.drawable.thumb_cp07, cells("cp07"), mask("cp07")),
            TemplateData("cp08", "CP08", R.drawable.temp_cp08, R.drawable.thumb_cp08, cells("cp08"), mask("cp08")),
            TemplateData("cp09", "CP09", R.drawable.temp_cp09, R.drawable.thumb_cp09, cells("cp09"), mask("cp09")),
            // cp10 chưa import: asset thiếu Thumb_CP10.png nên không dò được ô ảnh
            // (nền cùng màu xám với ô, không có bản lồng ảnh để đối chiếu).
            TemplateData("cp11", "CP11", R.drawable.temp_cp11, R.drawable.thumb_cp11, cells("cp11"), mask("cp11")),
            TemplateData("cp12", "CP12", R.drawable.temp_cp12, R.drawable.thumb_cp12, cells("cp12"), mask("cp12")),
            TemplateData("cp13", "CP13", R.drawable.temp_cp13, R.drawable.thumb_cp13, cells("cp13"), mask("cp13")),
            TemplateData("cp14", "CP14", R.drawable.temp_cp14, R.drawable.thumb_cp14, cells("cp14"), mask("cp14")),
            TemplateData("cp15", "CP15", R.drawable.temp_cp15, R.drawable.thumb_cp15, cells("cp15"), mask("cp15")),
            TemplateData("cp16", "CP16", R.drawable.temp_cp16, R.drawable.thumb_cp16, cells("cp16"), mask("cp16")),
            TemplateData("cp17", "CP17", R.drawable.temp_cp17, R.drawable.thumb_cp17, cells("cp17"), mask("cp17")),
            TemplateData("cp18", "CP18", R.drawable.temp_cp18, R.drawable.thumb_cp18, cells("cp18"), mask("cp18")),
            TemplateData("cp19", "CP19", R.drawable.temp_cp19, R.drawable.thumb_cp19, cells("cp19"), mask("cp19")),
            TemplateData("cp20", "CP20", R.drawable.temp_cp20, R.drawable.thumb_cp20, cells("cp20"), mask("cp20")),

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
            TemplateData("gs11", "GS11", R.drawable.temp_gs11, R.drawable.thumb_gs11, cells("gs11"), mask("gs11")),
            TemplateData("gs12", "GS12", R.drawable.temp_gs12, R.drawable.thumb_gs12, cells("gs12"), mask("gs12")),
            TemplateData("gs13", "GS13", R.drawable.temp_gs13, R.drawable.thumb_gs13, cells("gs13"), mask("gs13")),
            TemplateData("gs14", "GS14", R.drawable.temp_gs14, R.drawable.thumb_gs14, cells("gs14"), mask("gs14")),
            TemplateData("gs15", "GS15", R.drawable.temp_gs15, R.drawable.thumb_gs15, cells("gs15"), mask("gs15")),
            TemplateData("gs16", "GS16", R.drawable.temp_gs16, R.drawable.thumb_gs16, cells("gs16"), mask("gs16")),
            TemplateData("gs17", "GS17", R.drawable.temp_gs17, R.drawable.thumb_gs17, cells("gs17"), mask("gs17")),
            TemplateData("gs18", "GS18", R.drawable.temp_gs18, R.drawable.thumb_gs18, cells("gs18"), mask("gs18")),
            TemplateData("gs19", "GS19", R.drawable.temp_gs19, R.drawable.thumb_gs19, cells("gs19"), mask("gs19")),
            TemplateData("gs20", "GS20", R.drawable.temp_gs20, R.drawable.thumb_gs20, cells("gs20"), mask("gs20")),

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
            // thumb_is10 import bị lệch design (3 khung) so với temp_is10 (2 khung) -> preview dùng temp_ cho khớp.
            // is10: nguon Thumb_IS10 truoc day khac design voi temp_is10 nen phai lay chinh
            // temp_ lam preview. Nay designer da xuat lai dung design -> co thumb_is10 rieng.
            TemplateData("is10", "IS10", R.drawable.temp_is10, R.drawable.thumb_is10, cells("is10"), mask("is10")),
            TemplateData("is11", "IS11", R.drawable.temp_is11, R.drawable.thumb_is11, cells("is11"), mask("is11")),
            TemplateData("is12", "IS12", R.drawable.temp_is12, R.drawable.thumb_is12, cells("is12"), mask("is12")),
            TemplateData("is13", "IS13", R.drawable.temp_is13, R.drawable.thumb_is13, cells("is13"), mask("is13")),
            TemplateData("is14", "IS14", R.drawable.temp_is14, R.drawable.thumb_is14, cells("is14"), mask("is14")),
            TemplateData("is15", "IS15", R.drawable.temp_is15, R.drawable.thumb_is15, cells("is15"), mask("is15")),
            TemplateData("is16", "IS16", R.drawable.temp_is16, R.drawable.thumb_is16, cells("is16"), mask("is16")),
            TemplateData("is17", "IS17", R.drawable.temp_is17, R.drawable.thumb_is17, cells("is17"), mask("is17")),
            // is18 chưa import: asset Temp_IS18.png trùng khít Thumb_IS18.png (cả hai đều là bản
            // đã lồng ảnh mẫu) -> không có bản khung rỗng để dùng. is19/is20 asset thiếu Temp_.

            // ── Summer vibe ───────────────────────────────────
            TemplateData("sm01", "SM01", R.drawable.temp_sm01, R.drawable.thumb_sm01, cells("sm01"), mask("sm01")),
            TemplateData("sm02", "SM02", R.drawable.temp_sm02, R.drawable.thumb_sm02, cells("sm02"), mask("sm02")),
            TemplateData("sm03", "SM03", R.drawable.temp_sm03, R.drawable.thumb_sm03, cells("sm03"), mask("sm03")),
            TemplateData("sm04", "SM04", R.drawable.temp_sm04, R.drawable.thumb_sm04, cells("sm04"), mask("sm04")),
            TemplateData("sm05", "SM05", R.drawable.temp_sm05, R.drawable.thumb_sm05, cells("sm05"), mask("sm05")),
            TemplateData("sm06", "SM06", R.drawable.temp_sm06, R.drawable.thumb_sm06, cells("sm06"), mask("sm06")),
            // thumb_sm07 (Thumb_SM07.png) giờ đã khớp layout puzzle temp_sm07 -> preview dùng thumb_ (mẫu điền ảnh hiking).
            TemplateData("sm07", "SM07", R.drawable.temp_sm07, R.drawable.thumb_sm07, cells("sm07"), mask("sm07")),
            TemplateData("sm08", "SM08", R.drawable.temp_sm08, R.drawable.thumb_sm08, cells("sm08"), mask("sm08")),
            TemplateData("sm09", "SM09", R.drawable.temp_sm09, R.drawable.thumb_sm09, cells("sm09"), mask("sm09")),
            TemplateData("sm10", "SM10", R.drawable.temp_sm10, R.drawable.thumb_sm10, cells("sm10"), mask("sm10")),
            // sm11 chưa import: asset thiếu Thumb_SM11.png (như cp10).
            TemplateData("sm12", "SM12", R.drawable.temp_sm12, R.drawable.thumb_sm12, cells("sm12"), mask("sm12")),
            TemplateData("sm13", "SM13", R.drawable.temp_sm13, R.drawable.thumb_sm13, cells("sm13"), mask("sm13")),
            TemplateData("sm14", "SM14", R.drawable.temp_sm14, R.drawable.thumb_sm14, cells("sm14"), mask("sm14")),
            TemplateData("sm15", "SM15", R.drawable.temp_sm15, R.drawable.thumb_sm15, cells("sm15"), mask("sm15")),
            TemplateData("sm16", "SM16", R.drawable.temp_sm16, R.drawable.thumb_sm16, cells("sm16"), mask("sm16")),
            TemplateData("sm17", "SM17", R.drawable.temp_sm17, R.drawable.thumb_sm17, cells("sm17"), mask("sm17")),
            TemplateData("sm18", "SM18", R.drawable.temp_sm18, R.drawable.thumb_sm18, cells("sm18"), mask("sm18")),
            TemplateData("sm19", "SM19", R.drawable.temp_sm19, R.drawable.thumb_sm19, cells("sm19"), mask("sm19")),
            TemplateData("sm20", "SM20", R.drawable.temp_sm20, R.drawable.thumb_sm20, cells("sm20"), mask("sm20")),

            // ── Sports ──
            TemplateData("sp01", "SP01", R.drawable.temp_sp01, R.drawable.thumb_sp01, cells("sp01"), mask("sp01")),
            TemplateData("sp02", "SP02", R.drawable.temp_sp02, R.drawable.thumb_sp02, cells("sp02"), mask("sp02")),
            TemplateData("sp03", "SP03", R.drawable.temp_sp03, R.drawable.thumb_sp03, cells("sp03"), mask("sp03")),
            TemplateData("sp04", "SP04", R.drawable.temp_sp04, R.drawable.thumb_sp04, cells("sp04"), mask("sp04")),
            TemplateData("sp05", "SP05", R.drawable.temp_sp05, R.drawable.thumb_sp05, cells("sp05"), mask("sp05")),
            TemplateData("sp06", "SP06", R.drawable.temp_sp06, R.drawable.thumb_sp06, cells("sp06"), mask("sp06")),
            TemplateData("sp07", "SP07", R.drawable.temp_sp07, R.drawable.thumb_sp07, cells("sp07"), mask("sp07")),
            TemplateData("sp08", "SP08", R.drawable.temp_sp08, R.drawable.thumb_sp08, cells("sp08"), mask("sp08")),
            TemplateData("sp09", "SP09", R.drawable.temp_sp09, R.drawable.thumb_sp09, cells("sp09"), mask("sp09")),
            TemplateData("sp10", "SP10", R.drawable.temp_sp10, R.drawable.thumb_sp10, cells("sp10"), mask("sp10")),
            TemplateData("sp11", "SP11", R.drawable.temp_sp11, R.drawable.thumb_sp11, cells("sp11"), mask("sp11")),
            TemplateData("sp12", "SP12", R.drawable.temp_sp12, R.drawable.thumb_sp12, cells("sp12"), mask("sp12")),
            TemplateData("sp13", "SP13", R.drawable.temp_sp13, R.drawable.thumb_sp13, cells("sp13"), mask("sp13")),
            TemplateData("sp14", "SP14", R.drawable.temp_sp14, R.drawable.thumb_sp14, cells("sp14"), mask("sp14")),
            // sp15 chưa import: 3 dải chia CHÉO, các ô là hình thang có dải y chồng nhau nên
            // RectF + góc xoay không biểu diễn được (rect bao trùm sẽ đè lên nhau).
            TemplateData("sp16", "SP16", R.drawable.temp_sp16, R.drawable.thumb_sp16, cells("sp16"), mask("sp16")),
            TemplateData("sp17", "SP17", R.drawable.temp_sp17, R.drawable.thumb_sp17, cells("sp17"), mask("sp17")),
            TemplateData("sp18", "SP18", R.drawable.temp_sp18, R.drawable.thumb_sp18, cells("sp18"), mask("sp18")),
            TemplateData("sp19", "SP19", R.drawable.temp_sp19, R.drawable.thumb_sp19, cells("sp19"), mask("sp19")),
            TemplateData("sp20", "SP20", R.drawable.temp_sp20, R.drawable.thumb_sp20, cells("sp20"), mask("sp20"))
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
