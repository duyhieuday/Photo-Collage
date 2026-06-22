package com.example.piceditor

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.piceditor.draft.DraftProject
import com.example.piceditor.draft.DraftRepository
import com.example.piceditor.templates_editor.TemplateEditorActivity
import com.example.piceditor.utilsApp.DraftStore
import com.example.piceditor.utilsApp.DraftType
import com.ezt.pdfreader.photoeditor.data.FilterType
import com.ezt.pdfreader.photoeditor.data.PageInfo
import java.io.File

/**
 * Mở lại đúng editor cho 1 item trong My Draft.
 *
 * Ưu tiên 1: có DỰ ÁN đã lưu ([DraftRepository]) → mở editor ở chế độ KHÔI PHỤC ĐẦY ĐỦ
 *            (ảnh gốc + bố cục + sticker/chữ/vẽ + transform).
 * Ưu tiên 2 (ảnh cũ chưa có dự án): fallback mở editor với ẢNH ĐÃ LƯU làm đầu vào, theo nhãn [DraftStore].
 *
 * LƯU Ý THREAD: [resolveIntent] có IO (đọc project.json, copy file) → gọi ở BACKGROUND thread,
 * rồi startActivity(intent) ở main. [open] là tiện ích đồng bộ (chỉ dùng khi không có coroutine scope).
 */
object DraftRouter {

    /** Dựng Intent mở editor phù hợp. CÓ IO (đọc file/parse/copy) → nên gọi ở background thread. */
    fun resolveIntent(context: Context, uri: Uri): Intent {
        val project = DraftRepository.load(context, uri)
        return when (project?.type) {
            DraftType.TEMPLATE.name -> templateResumeIntent(context, uri)
            DraftType.COLLAGE.name -> collageResumeIntent(context, uri)
            DraftType.AI.name -> aiResumeIntent(context, uri)
            DraftType.EDIT.name -> editResumeIntent(context, uri, project)
            else -> fallbackIntent(context, uri)
        }
    }

    /** Tiện ích đồng bộ (chạy IO trên thread gọi). Dùng khi không có scope nền. */
    fun open(context: Context, uri: Uri) {
        context.startActivity(resolveIntent(context, uri))
    }

    // ── Khôi phục đầy đủ từ dự án đã lưu ──────────────────────────────

    private fun templateResumeIntent(context: Context, uri: Uri) =
        Intent(context, TemplateEditorActivity::class.java).apply {
            putExtra(TemplateEditorActivity.EXTRA_DRAFT_URI, uri.toString())
        }

    private fun collageResumeIntent(context: Context, uri: Uri) =
        Intent(context, CollageActivity::class.java).apply {
            putExtra(CollageActivity.EXTRA_DRAFT_URI, uri.toString())
        }

    private fun aiResumeIntent(context: Context, uri: Uri) =
        Intent(context, AfterRemoveActivity::class.java).apply {
            putExtra(AfterRemoveActivity.EXTRA_DRAFT_URI, uri.toString())
        }

    private fun editResumeIntent(context: Context, uri: Uri, project: DraftProject): Intent {
        val edit = project.edit
        if (edit == null || edit.pages.isEmpty()) return editFallbackIntent(context, uri)
        val pages = edit.pages.map { p ->
            PageInfo(
                uri = Uri.fromFile(File(p.imagePath)),
                corners = p.corners?.toIntArray(),
                filterType = runCatching { FilterType.valueOf(p.filter) }.getOrDefault(FilterType.NONE),
                rotation = p.rotation,
                flipX = p.flipX,
                flipY = p.flipY,
                brightness = p.brightness,
                contrast = p.contrast,
                saturation = p.saturation,
                warmth = p.warmth
            )
        }
        return PhotoEditorWithBannerActivity.createIntent(context, pages)
    }

    // ── Fallback: ảnh cũ chưa có dự án → mở tool với ảnh đã lưu ────────

    private fun fallbackIntent(context: Context, uri: Uri): Intent {
        val key = uri.toString()
        return when (DraftStore.typeOf(context, key) ?: DraftType.EDIT) {
            DraftType.EDIT -> editFallbackIntent(context, uri)
            DraftType.AI -> Intent(context, AiRemoveActivity::class.java).apply {
                putExtra(AiRemoveActivity.EXTRA_IMAGE_PATH, uri.toString())
            }
            DraftType.COLLAGE -> Intent(context, CollageActivity::class.java).apply {
                putExtra("imageCount", 1)
                putExtra("imagesinTemplate", 1)
                putStringArrayListExtra("selectedImages", arrayListOf(resolveToFilePath(context, uri)))
            }
            DraftType.TEMPLATE -> Intent(context, TemplateEditorActivity::class.java).apply {
                DraftStore.templateIdOf(context, key)?.let { putExtra(TemplateEditorActivity.EXTRA_TEMPLATE_ID, it) }
                putExtra(TemplateEditorActivity.EXTRA_PRELOAD_IMAGE, uri.toString())
            }
        }
    }

    private fun editFallbackIntent(context: Context, uri: Uri) =
        PhotoEditorWithBannerActivity.createIntent(context, listOf(PageInfo(uri)))

    /**
     * Collage decode bằng BitmapFactory.decodeFile nên cần đường dẫn file thật.
     * Copy content:// ra 1 file cache (an toàn trên mọi phiên bản Android). CÓ IO → gọi ở background.
     */
    private fun resolveToFilePath(context: Context, uri: Uri): String = runCatching {
        val out = File(context.cacheDir, "draft_src_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { input.copyTo(it) }
        } ?: throw IllegalStateException("Cannot open $uri")
        out.absolutePath
    }.getOrDefault(uri.path ?: uri.toString())
}
