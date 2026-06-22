package com.example.piceditor.draft

import android.content.Context
import android.net.Uri
import com.example.piceditor.draw.model.DrawData
import com.google.gson.Gson
import java.io.File

/**
 * "Dự án" của 1 draft — đủ để mở lại editor đúng như lúc người dùng đang làm
 * (ảnh gốc + bố cục + sticker/chữ/vẽ/nền + transform), KHÔNG phải ảnh đã ghép phẳng.
 *
 * Mỗi draft lưu trong 1 thư mục riêng (filesDir/drafts/<key>/) gồm:
 *  - project.json  : object [DraftProject] này
 *  - *.jpg/.png    : các ảnh gốc + ảnh sticker đã copy vào (để bền, không phụ thuộc cache/gallery)
 *
 * Key gắn theo URI ảnh export trong My Draft → bấm item là tra ra đúng dự án.
 */
data class DraftProject(
    val type: String,            // COLLAGE | AI | EDIT | TEMPLATE (DraftType.name)
    val createdAt: Long = 0L,
    /** Trạng thái sticker + chữ + nét vẽ + nền (DrawData serialize bằng Gson). */
    val drawDataJson: String? = null,
    val collage: CollageDraft? = null,
    val template: TemplateDraft? = null,
    val edit: EditDraft? = null,
    val ai: AiDraft? = null,
)

data class CollageDraft(
    val frameTitle: String,           // TemplateItem.title (vd "collage_3_2.png") để chọn lại đúng khung
    val imagePaths: List<String>,     // ảnh gốc từng ô (đã copy vào folder draft)
    val space: Float = 0f,
    val corner: Float = 0f,
    val backgroundColor: Int = android.graphics.Color.WHITE,
    val rotation: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
)

data class TemplateDraft(
    // Nullable + default để Gson không gán null vào field non-null khi project.json cũ/hỏng/thiếu key.
    val templateId: String? = null,
    val cellImagePaths: List<String?>? = null,    // ảnh gốc từng ô (null = ô trống); index khớp cells
    val cellMatrices: List<List<Float>?>? = null, // matrix 9 phần tử từng ô (pan/zoom trong ô); null nếu trống
)

data class EditDraft(
    val pages: List<EditPageDraft>,
)

data class EditPageDraft(
    val imagePath: String,            // ảnh gốc (đã copy vào folder draft)
    val corners: List<Int>? = null,
    val filter: String = "NONE",      // FilterType.name
    val rotation: Int = 0,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 1f,
)

data class AiDraft(
    val subjectPath: String,          // ảnh subject (đã copy vào folder draft)
    val rotation: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
)

/**
 * Lưu / nạp dự án draft theo URI ảnh export. Tự copy ảnh vào thư mục draft để bền.
 */
object DraftRepository {

    private const val DRAFTS_DIR = "drafts"
    private const val PROJECT_FILE = "project.json"
    private val gson = Gson()

    fun keyFor(exportUri: Uri): String =
        Integer.toHexString(exportUri.toString().hashCode()) + "_" +
            exportUri.lastPathSegment.orEmpty().filter { it.isLetterOrDigit() }

    /** Thư mục draft cho 1 URI export (tạo nếu chưa có). */
    fun dirFor(context: Context, exportUri: Uri): File =
        File(File(context.filesDir, DRAFTS_DIR), keyFor(exportUri)).apply { mkdirs() }

    /**
     * Copy 1 ảnh (content:// hoặc đường dẫn file) vào thư mục draft, trả về absolute path bản copy.
     * Trả null nếu copy lỗi.
     */
    fun stashImage(context: Context, dir: File, src: Uri, name: String): String? = runCatching {
        val out = File(dir, name)
        context.contentResolver.openInputStream(src)?.use { input ->
            out.outputStream().use { input.copyTo(it) }
        } ?: run {
            // Có thể là đường dẫn file thuần
            File(src.path ?: src.toString()).inputStream().use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
        }
        out.absolutePath
    }.getOrNull()

    fun stashImagePath(dir: File, srcPath: String, name: String): String? = runCatching {
        val srcFile = File(if (srcPath.startsWith("file://")) Uri.parse(srcPath).path ?: srcPath else srcPath)
        if (!srcFile.exists()) return@runCatching null
        val out = File(dir, name)
        srcFile.inputStream().use { input -> out.outputStream().use { input.copyTo(it) } }
        out.absolutePath
    }.getOrNull()

    fun save(dir: File, project: DraftProject) {
        runCatching {
            File(dir, PROJECT_FILE).writeText(gson.toJson(project))
        }
    }

    fun load(context: Context, exportUri: Uri): DraftProject? = runCatching {
        val f = File(File(File(context.filesDir, DRAFTS_DIR), keyFor(exportUri)), PROJECT_FILE)
        if (!f.exists()) null else gson.fromJson(f.readText(), DraftProject::class.java)
    }.getOrNull()

    fun delete(context: Context, exportUri: Uri) {
        runCatching {
            File(File(context.filesDir, DRAFTS_DIR), keyFor(exportUri)).deleteRecursively()
        }
    }
}

/**
 * Lưu/khôi phục state sticker + chữ + nét vẽ + nền (DrawData) cho collage/ai/template.
 * Sticker chữ là file ở cache (dễ bị xoá) → copy vào folder draft và đổi path để bền.
 * Sticker asset (assets://...) giữ nguyên path, sẽ tự load lại từ assets.
 */
object DraftDrawData {

    private val gson = Gson()

    /** Serialize DrawData, đồng thời copy các ảnh sticker dạng FILE vào [dir]. */
    fun stash(context: Context, dir: File, data: DrawData?): String? {
        if (data == null) return null
        // Deep copy để không sửa state đang hiển thị
        val copy = data.deepCopy()
        copy.listSticker.forEachIndexed { i, sticker ->
            val path = sticker.pathImage ?: return@forEachIndexed
            val srcFile = File(path)
            if (srcFile.exists()) {
                val stashed = DraftRepository.stashImagePath(dir, path, "sticker_${i}_${srcFile.name}")
                if (stashed != null) sticker.pathImage = stashed
            }
        }
        return runCatching { gson.toJson(copy) }.getOrNull()
    }

    fun parse(json: String?): DrawData? =
        if (json.isNullOrEmpty()) null
        else runCatching { gson.fromJson(json, DrawData::class.java) }.getOrNull()
}
