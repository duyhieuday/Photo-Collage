package com.example.piceditor.templates_editor

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IntDef
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piceditor.R
import com.example.piceditor.adapters.BackgroundAdapter
import com.example.piceditor.adapters.ToolAdapter
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityTemplateEditorBinding
import com.example.piceditor.draw.DrawInteractListener
import com.example.piceditor.draw.model.draw.DrawPath
import com.example.piceditor.draw.model.draw.style.BrushStyle
import com.example.piceditor.draw.model.draw.style.PaintStyle
import com.example.piceditor.draw.model.sticker.StickerData
import com.example.piceditor.draw.test.Beard
import com.example.piceditor.draw.test.BeardAdapter
import com.example.piceditor.model.ToolItem
import com.example.piceditor.utils.BarsUtils
import com.example.piceditor.utils.ImageUtils
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStreamReader
import kotlin.math.min

class TemplateEditorActivity : BaseActivityNew<ActivityTemplateEditorBinding>(),
    BackgroundAdapter.OnBGClickListener,
    DrawInteractListener {

    companion object {
        const val EXTRA_TEMPLATE_ID = "extra_template_id"
        const val TYPE_GESTURE = 0
        const val TYPE_SHAPE   = 1
        const val TYPE_ERASER  = 2
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(TYPE_GESTURE, TYPE_SHAPE, TYPE_ERASER)
    annotation class DrawType

    // ── Template state ─────────────────────────────────────
    private var selectedCell: PhotoCell? = null
    private lateinit var templateData: TemplateData
    private val TEMPLATE_W = 1125f
    private val TEMPLATE_H = 2000f

    // ── Draw / sticker state ───────────────────────────────
    @DrawType private var drawType = TYPE_GESTURE
    private var drawColor: Int = Color.BLACK
    private var gestureSize: Float = 16f
    private var eraserSize: Float  = 20f
    private var gesturePaintStyle: PaintStyle = PaintStyle.STROKE

    // ── Sticker ────────────────────────────────────────────
    private var beardAdapter: BeardAdapter? = null

    // ── Background ─────────────────────────────────────────
    private var backgroundBitmap: Bitmap? = null

    // ── Gallery launcher ───────────────────────────────────
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            loadImageFromUri(uri)
        }

    // ── BaseActivityNew overrides ──────────────────────────
    override fun getLayoutRes(): Int = R.layout.activity_template_editor
    override fun getFrame(): Int = 0
    override fun getDataFromIntent() {}
    override fun setListener() {}
    override fun initFragment(): BaseFragment<*>? = null
    override fun doAfterOnCreate() {
        if (PreferenceUtil.getInstance(this)
                .getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")) {
            initBanner(binding.banner.adViewContainer)
        } else {
            initBanner(binding.adViewContainer)
            binding.banner.root.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (!PreferenceUtil.getInstance(this)
                .getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")) {
            initBanner(binding.adViewContainer)
            binding.banner.root.visibility = View.GONE
        }
    }

    override fun afterSetContentView() {
        super.afterSetContentView()
        BarsUtils.setHideNavigation(this)
        BarsUtils.setStatusBarColor(this, "#01000000".toColorInt())
        BarsUtils.setAppearanceLightStatusBars(this, true)
    }

    // ──────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val templateId = intent.getStringExtra(EXTRA_TEMPLATE_ID)
            ?: TemplateRepository.all.first().id
        templateData = TemplateRepository.findById(templateId)
            ?: TemplateRepository.all.first()

        // DrawView tắt drawing mặc định, chỉ bật khi vào tab sticker/draw
        binding.templateEditorView.drawView?.setDrawingEnabled(false)

        setupToolTabs()
        setupListeners()
        loadTemplate()

        // Đăng ký undo/redo listener
        binding.templateEditorView.drawView?.drawManager?.addDrawInteractListener(this)
        syncUndoRedoUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.templateEditorView.cells.forEach { cell ->
            cell.bitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    }

    // ──────────────────────────────────────────────────────
    // Undo / Redo
    // ──────────────────────────────────────────────────────

    private fun syncUndoRedoUI() {
        val manager = binding.templateEditorView.drawView?.drawManager ?: return
        val canUndo = manager.isActiveUndo
        val canRedo = manager.isActiveRedo

        binding.btnUndo.isEnabled   = canUndo
        binding.btnUndo.alpha       = if (canUndo) 1f else 0.3f
        binding.btnUndo.colorFilter = if (canUndo) null
        else ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })

        binding.btnRedo.isEnabled   = canRedo
        binding.btnRedo.alpha       = if (canRedo) 1f else 0.3f
        binding.btnRedo.colorFilter = if (canRedo) null
        else ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    }

    override fun interactUndoRedoChange() { syncUndoRedoUI() }
    override fun interactStickerFocusChange(stickerData: StickerData?) {}
    override fun interactTouchDown() {}
    override fun interactTouchUp() {}
    override fun interactUpdateBackground(url: String?) {}

    // ──────────────────────────────────────────────────────
    // Background callback
    // ──────────────────────────────────────────────────────

    override fun onBGClick(drawable: Drawable) {
        val bitmap = (drawable as BitmapDrawable).bitmap
        backgroundBitmap = bitmap
        binding.templateEditorView.setBackgroundBitmap(bitmap)
    }

    // ──────────────────────────────────────────────────────
    // Setup
    // ──────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnExport.setOnClickListener { onExportClick() }

        binding.btnUndo.setOnClickListener {
            binding.templateEditorView.drawView?.drawManager?.undo()
        }
        binding.btnRedo.setOnClickListener {
            binding.templateEditorView.drawView?.drawManager?.redo()
        }

        // Tap cell trống → mở gallery
        binding.templateEditorView.setOnCellClickListener { cell ->
            selectedCell = cell
            openGallery()
        }
    }

    private fun setupToolTabs() {
        val tools = mutableListOf(
            ToolItem(R.drawable.ic_replace_image, getString(R.string.replace)),
            ToolItem(R.drawable.ic_border,       getString(R.string.border)),
            ToolItem(R.drawable.ic_sticker,      getString(R.string.sticker)),
            ToolItem(R.drawable.ic_background,   getString(R.string.background)),
            ToolItem(R.drawable.ic_text,         getString(R.string.text)),
        )

        val adapter = ToolAdapter(tools) { _, pos ->
            binding.llPhoto.visibility   = View.GONE
            binding.llBorder.visibility  = View.GONE
            binding.llSticker.visibility = View.GONE
            binding.llBg.visibility      = View.GONE
            binding.llDraw.visibility    = View.GONE
            binding.rcvTools.visibility  = View.GONE
            binding.templateEditorView.drawView?.setDrawingEnabled(false)

            when (pos) {
                0 -> { // Ảnh
                    binding.llPhoto.visibility = View.VISIBLE
                    refreshPhotoCellList()
                    binding.icCheckPhoto.setOnClickListener {
                        binding.llPhoto.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }
                }
                1 -> { // Border
                    binding.llBorder.visibility = View.VISIBLE
                    binding.tvGrid.text   = binding.seekbarSpace.progress.toString()
                    binding.tvCorner.text = binding.seekbarCorner.progress.toString()
                    binding.icCheckBorder.setOnClickListener {
                        binding.llBorder.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }
                    setupBorderSeekbars()
                }
                2 -> { // Sticker
                    binding.llSticker.visibility = View.VISIBLE
                    loadStickers()
                    binding.icCheckSticker.setOnClickListener {
                        binding.llSticker.visibility = View.GONE
                        binding.rcvTools.visibility  = View.VISIBLE
                        binding.templateEditorView.drawView?.setDrawingEnabled(false)
                    }
                    binding.templateEditorView.drawView?.setDrawingEnabled(true)
                    syncUndoRedoUI()
                }
                3 -> { // Background
                    binding.llBg.visibility = View.VISIBLE
                    binding.listBg.layoutManager =
                        LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                    binding.listBg.adapter = BackgroundAdapter(this, this)
                    binding.icCheckBackground.setOnClickListener {
                        binding.llBg.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }
                }
                4 -> { // Text / Draw
                    binding.llDraw.visibility = View.VISIBLE
                    binding.icBrush.setOnClickListener {
                        binding.icBrush.setBackgroundResource(R.drawable.bg_icon_draw)
                        binding.icErase.setBackgroundResource(0)
                        drawType = TYPE_GESTURE
                        updateDraw()
                    }
                    binding.icErase.setOnClickListener {
                        binding.icErase.setBackgroundResource(R.drawable.bg_icon_draw)
                        binding.icBrush.setBackgroundResource(0)
                        drawType = TYPE_ERASER
                        updateDraw()
                    }
                    binding.icCheckDraw.setOnClickListener {
                        binding.llDraw.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                        binding.templateEditorView.drawView?.setDrawingEnabled(false)
                    }
                    updateDraw()
                    syncUndoRedoUI()
                }
            }
        }

        binding.rcvTools.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvTools.adapter = adapter
    }

    // ──────────────────────────────────────────────────────
    // Border seekbars
    // ──────────────────────────────────────────────────────

    private fun setupBorderSeekbars() {
        binding.seekbarSpace.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvGrid.text = progress.toString()
                val space = ImageUtils.pxFromDp(this@TemplateEditorActivity, progress * 0.1f)
                binding.templateEditorView.setCellSpacing(space)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.seekbarCorner.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvCorner.text = progress.toString()
                val corner = ImageUtils.pxFromDp(this@TemplateEditorActivity, progress * 0.3f)
                binding.templateEditorView.setCellCorner(corner)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    // ──────────────────────────────────────────────────────
    // Draw
    // ──────────────────────────────────────────────────────

    private fun updateDraw() {
        gesturePaintStyle = PaintStyle.STROKE
        val drawPath = when (drawType) {
            TYPE_GESTURE -> DrawPath(BrushStyle.GESTURE, gesturePaintStyle, drawColor, gestureSize)
            TYPE_ERASER  -> DrawPath(BrushStyle.GESTURE, PaintStyle.ERASE,  drawColor, eraserSize)
            else         -> DrawPath(BrushStyle.GESTURE, gesturePaintStyle, drawColor, gestureSize)
        }
        binding.templateEditorView.drawView?.drawManager?.setDrawPath(drawPath)
        binding.icBrush.isSelected = drawType == TYPE_GESTURE
        binding.icErase.isSelected = drawType == TYPE_ERASER
        binding.templateEditorView.drawView?.setDrawingEnabled(true)
    }

    // ──────────────────────────────────────────────────────
    // Photo cell list
    // ──────────────────────────────────────────────────────

    private fun refreshPhotoCellList() {
        val cells = binding.templateEditorView.cells
        val adapter = PhotoCellAdapter(cells) { cell ->
            // User tap vào 1 cell trong panel → chọn cell đó và mở gallery
            selectedCell = cell
            openGallery()
        }
        binding.listPhotoCell.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.listPhotoCell.adapter = adapter
    }

    // ──────────────────────────────────────────────────────
    // Stickers
    // ──────────────────────────────────────────────────────

    private fun loadStickers() {
        if (beardAdapter != null) return // đã load rồi
        val gson  = Gson()
        val type  = object : TypeToken<MutableList<Beard?>?>() {}.getType()
        val beards: MutableList<Beard?>? = try {
            gson.fromJson(InputStreamReader(assets.open("beard.json")), type)
        } catch (e: IOException) { null }

        binding.listSticker.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        beardAdapter = BeardAdapter()
        beardAdapter?.setData(beards)
        binding.listSticker.adapter = beardAdapter
        beardAdapter?.setClickListener { _, beard ->
            binding.templateEditorView.drawView?.drawManager
                ?.addSticker(StickerData(beard.imageAsset))
        }
    }

    // ──────────────────────────────────────────────────────
    // Template loading
    // ──────────────────────────────────────────────────────

    private fun loadTemplate() {
        // Đợi view measure xong mới lấy kích thước thực
        binding.templateEditorView.post {
            val viewW = binding.templateEditorView.width.toFloat()
            val viewH = binding.templateEditorView.height.toFloat()

            lifecycleScope.launch {
                val (scaled, mask) = withContext(Dispatchers.IO) {
                    val raw   = BitmapFactory.decodeResource(resources, templateData.drawableRes)
                    // ✅ Scale theo editorView thực tế, không phải screen size
                    val scale = min(viewW / TEMPLATE_W, viewH / TEMPLATE_H)
                    val newW  = (TEMPLATE_W * scale).toInt()
                    val newH  = (TEMPLATE_H * scale).toInt()
                    val scaled = raw.scale(newW, newH)
                    raw.recycle()
                    val mask = if (templateData.maskMode == MaskMode.BLACK)
                        binding.templateEditorView.createMaskFromBlack(scaled)
                    else
                        binding.templateEditorView.createMaskFromWhite(scaled)
                    Pair(scaled, mask)
                }
                binding.templateEditorView.templateBitmapRaw  = scaled
                binding.templateEditorView.templateMaskBitmap = mask

                // ✅ Setup cell dùng cùng viewW/viewH — không cần post lại
                val scale = min(viewW / TEMPLATE_W, viewH / TEMPLATE_H)
                val newW  = TEMPLATE_W * scale
                val newH  = TEMPLATE_H * scale
                val dx    = (viewW - newW) / 2f
                val dy    = (viewH - newH) / 2f

                binding.templateEditorView.cells = templateData.cellRects.map { rect ->
                    PhotoCell(RectF(
                        rect.left   * scale + dx,
                        rect.top    * scale + dy,
                        rect.right  * scale + dx,
                        rect.bottom * scale + dy
                    ))
                }.toMutableList()

                binding.templateEditorView.invalidate()
            }
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
            selectedCell?.let { binding.templateEditorView.setImageToCell(it, bitmap) }
                ?: Toast.makeText(this@TemplateEditorActivity, "Chưa chọn ô ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    // ──────────────────────────────────────────────────────
    // Export
    // ──────────────────────────────────────────────────────

    private fun onExportClick() {
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching { saveToGallery(binding.templateEditorView.export()) }.isSuccess
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
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoCollage")
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
                .resolve("PhotoCollage").also { it.mkdirs() }
            val file = dir.resolve(filename)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            @Suppress("DEPRECATION")
            MediaScannerConnection.scanFile(
                this, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null
            )
        }
    }

    private fun dipToPx(dip: Int): Int =
        (dip * resources.displayMetrics.density).toInt()
}