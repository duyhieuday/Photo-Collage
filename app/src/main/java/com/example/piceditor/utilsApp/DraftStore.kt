package com.example.piceditor.utilsApp

import android.content.Context

/** Tool tạo ra 1 draft (ảnh đã export trong Pictures/PhotoCollage). */
enum class DraftType { COLLAGE, AI, EDIT, TEMPLATE }

/**
 * Lưu nhãn "draft này tạo từ tool nào" để khi bấm vào item trong My Draft
 * mở lại đúng editor (collage / ai / edit / template).
 *
 * Key dùng [android.net.Uri.toString] của ảnh trong MediaStore. Cả 4 luồng save đều
 * insert() ra dạng `content://media/external/images/media/<id>` — trùng đúng URI mà
 * MyDraftActivity build lại từ id khi query — nên key khớp tuyệt đối.
 *
 * Chỉ gắn nhãn cho ảnh tạo TỪ GIỜ; ảnh cũ không có nhãn → mặc định mở ở Edit
 * (xem [com.example.piceditor.DraftRouter]).
 */
object DraftStore {
    private const val PREFS = "draft_types"
    private const val TEMPLATE_KEY_PREFIX = "tpl::" // lưu kèm template id cho draft template

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Gắn nhãn tool cho 1 draft. Với template truyền thêm [templateId] để mở lại đúng layout. */
    fun tag(context: Context, uri: String, type: DraftType, templateId: String? = null) {
        prefs(context).edit().apply {
            putString(uri, type.name)
            if (templateId != null) putString(TEMPLATE_KEY_PREFIX + uri, templateId)
            apply()
        }
    }

    /** Loại tool đã tạo draft, hoặc null nếu chưa từng gắn nhãn (ảnh cũ). */
    fun typeOf(context: Context, uri: String): DraftType? =
        prefs(context).getString(uri, null)
            ?.let { runCatching { DraftType.valueOf(it) }.getOrNull() }

    /** Template id đã lưu kèm (chỉ có với draft template). */
    fun templateIdOf(context: Context, uri: String): String? =
        prefs(context).getString(TEMPLATE_KEY_PREFIX + uri, null)
}
