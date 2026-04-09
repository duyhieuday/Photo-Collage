package com.example.piceditor.templates_editor

import android.content.ContentValues
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.piceditor.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

class TemplateEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TEMPLATE_ID = "extra_template_id"
    }

    // ── Views ──────────────────────────────────────────────
    private lateinit var editorView: TemplateEditorView
    private lateinit var btnBack: ImageButton
    private lateinit var btnExport: Button
    private lateinit var btnChangePhoto: LinearLayout
    private lateinit var btnReset: LinearLayout
    private lateinit var tvTemplateName: TextView

    // ── State ──────────────────────────────────────────────
    private var selectedCell: PhotoCell? = null
    private lateinit var templateData: TemplateData

    private val TEMPLATE_W = 1125f
    private val TEMPLATE_H = 2000f

    // ── Gallery launcher ───────────────────────────────────
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            loadImageFromUri(uri)
        }

    // ──────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Dùng layout XML thay vì setContentView(editorView)
        setContentView(R.layout.activity_template_editor)

        // Bind views
        editorView      = findViewById(R.id.templateEditorView)
        btnBack         = findViewById(R.id.btnBack)
        btnExport       = findViewById(R.id.btnExport)
        btnChangePhoto  = findViewById(R.id.btnChangePhoto)
        btnReset        = findViewById(R.id.btnReset)
        tvTemplateName  = findViewById(R.id.tvTemplateName)

        // Lấy template từ intent
        val templateId = intent.getStringExtra(EXTRA_TEMPLATE_ID)
            ?: TemplateRepository.all.first().id
        templateData = TemplateRepository.findById(templateId)
            ?: TemplateRepository.all.first()

        tvTemplateName.text = templateData.name

        setupListeners()
        loadTemplate()
    }

    override fun onDestroy() {
        super.onDestroy()
        editorView.cells.forEach { cell ->
            cell.bitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    }

    // ──────────────────────────────────────────────────────
    // Setup
    // ──────────────────────────────────────────────────────

    private fun setupListeners() {
        // Back
        btnBack.setOnClickListener { finish() }

        // Export
        btnExport.setOnClickListener { onExportClick() }

        // Thay ảnh (tap nút bottom bar)
        btnChangePhoto.setOnClickListener {
            val cell = editorView.activeCell ?: run {
                Toast.makeText(this, "Hãy tap vào ô ảnh trước", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            selectedCell = cell
            openGallery()
        }

        // Reset vị trí ảnh của cell đang active
        btnReset.setOnClickListener {
            val cell = editorView.activeCell ?: run {
                Toast.makeText(this, "Hãy tap vào ô ảnh trước", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            cell.bitmap?.let {
                editorView.setImageToCell(cell, it) // reset về fit-center-crop mặc định
            }
        }

        // Tap cell → mở gallery nếu cell chưa có ảnh
        editorView.setOnCellClickListener { cell ->
            selectedCell = cell
            openGallery()
        }
    }

    private fun loadTemplate() {
        lifecycleScope.launch {
            val (scaled, mask) = withContext(Dispatchers.IO) {
                val raw = BitmapFactory.decodeResource(resources, templateData.drawableRes)

                // Dùng kích thước thực của editorView sau khi layout xong
                // Fallback về screen size nếu view chưa measure
                val viewW = resources.displayMetrics.widthPixels.toFloat()
                val viewH = (resources.displayMetrics.heightPixels - dipToPx(56 + 64)).toFloat()

                val scale = min(viewW / TEMPLATE_W, viewH / TEMPLATE_H)
                val newW = (TEMPLATE_W * scale).toInt()
                val newH = (TEMPLATE_H * scale).toInt()

                val scaled = Bitmap.createScaledBitmap(raw, newW, newH, true)
                raw.recycle()

                val mask = if (templateData.maskMode == MaskMode.BLACK) {
                    editorView.createMaskFromBlack(scaled)
                } else {
                    editorView.createMaskFromWhite(scaled)
                }
                Pair(scaled, mask)
            }

            editorView.templateBitmapRaw  = scaled
            editorView.templateMaskBitmap = mask
            setupCells()
        }
    }

    // ──────────────────────────────────────────────────────
    // Cells
    // ──────────────────────────────────────────────────────

    private fun setupCells() {
        // Đợi editorView được measure xong
        editorView.post {
            val viewW = editorView.width.toFloat()
            val viewH = editorView.height.toFloat()

            val scale = min(viewW / TEMPLATE_W, viewH / TEMPLATE_H)
            val newW = TEMPLATE_W * scale
            val newH = TEMPLATE_H * scale
            val dx = (viewW - newW) / 2f
            val dy = (viewH - newH) / 2f

            editorView.cells = templateData.cellRects.map { rect ->
                PhotoCell(
                    RectF(
                        rect.left   * scale + dx,
                        rect.top    * scale + dy,
                        rect.right  * scale + dx,
                        rect.bottom * scale + dy
                    )
                )
            }.toMutableList()

            editorView.invalidate()
        }
    }

    // ──────────────────────────────────────────────────────
    // Gallery
    // ──────────────────────────────────────────────────────

    private fun openGallery() { pickImageLauncher.launch("image/*") }

    private fun loadImageFromUri(uri: Uri) {
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    }
                }.getOrNull()
            }

            if (bitmap == null) {
                Toast.makeText(this@TemplateEditorActivity, "Không thể tải ảnh", Toast.LENGTH_SHORT).show()
                return@launch
            }

            selectedCell?.let { editorView.setImageToCell(it, bitmap) }
                ?: Toast.makeText(this@TemplateEditorActivity, "Chưa chọn ô ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    // ──────────────────────────────────────────────────────
    // Export
    // ──────────────────────────────────────────────────────

    private fun onExportClick() {
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching { saveToGallery(editorView.export()) }.isSuccess
            }
            Toast.makeText(
                this@TemplateEditorActivity,
                if (saved) "✅ Đã lưu vào thư viện!" else "❌ Lưu thất bại",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveToGallery(bitmap: Bitmap) {
        val filename = "collage_${System.currentTimeMillis()}.jpg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PicEditor")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: throw Exception("Insert failed")
            contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .resolve("PicEditor").also { it.mkdirs() }
            val file = dir.resolve(filename)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            @Suppress("DEPRECATION")
            MediaScannerConnection.scanFile(
                this, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null
            )
        }
    }

    // ──────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────

    private fun dipToPx(dip: Int): Int =
        (dip * resources.displayMetrics.density).toInt()
}