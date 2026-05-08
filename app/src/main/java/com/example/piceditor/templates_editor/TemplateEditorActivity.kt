package com.example.piceditor.templates_editor

import android.content.Intent
import android.content.ContentValues
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IntDef
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piceditor.R
import com.example.piceditor.adapters.BackgroundAdapter
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
import com.example.piceditor.adapters.ToolAdapter
import com.example.piceditor.model.ToolItem
import com.example.piceditor.utils.BarsUtils
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStreamReader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.example.piceditor.ShowImageActivity
import androidx.core.graphics.scale

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

    // Width chuẩn của logic space — height sẽ tính theo aspect ratio bitmap thật
    private val TEMPLATE_W = 1125f

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

    // ── Template loading flag ──────────────────────────────
    private var templateLoaded = false

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

        // DrawView setup
        binding.templateEditorView.drawView?.setDrawingEnabled(false)
        binding.templateEditorView.drawView?.setGestureEnabled(true)

        setupToolTabs()
        setupListeners()
        loadTemplate()

        binding.templateEditorView.drawView?.drawManager?.addDrawInteractListener(this)
        syncUndoRedoUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.templateEditorView.cells.forEach { cell ->
            cell.bitmap?.takeIf { !it.isRecycled }?.recycle()
        }
        binding.templateEditorView.templateBitmapRaw?.takeIf { !it.isRecycled }?.recycle()
        binding.templateEditorView.templateMaskBitmap?.takeIf { !it.isRecycled }?.recycle()
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
            binding.templateEditorView.drawView?.setGestureEnabled(true)

            when (pos) {
                0 -> {
                    binding.llPhoto.visibility = View.VISIBLE
                    refreshPhotoCellList()
                    binding.icCheckPhoto.setOnClickListener {
                        binding.llPhoto.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }
                }
                1 -> {
                    binding.llBorder.visibility = View.VISIBLE
                    binding.tvGrid.text   = binding.seekbarSpace.progress.toString()
                    binding.tvCorner.text = binding.seekbarCorner.progress.toString()
                    binding.icCheckBorder.setOnClickListener {
                        binding.llBorder.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }
                    setupBorderSeekbars()
                }
                2 -> {
                    binding.llSticker.visibility = View.VISIBLE
                    loadStickers()
                    binding.icCheckSticker.setOnClickListener {
                        binding.llSticker.visibility = View.GONE
                        binding.rcvTools.visibility  = View.VISIBLE
                        binding.templateEditorView.drawView?.setDrawingEnabled(false)
                        binding.templateEditorView.drawView?.setGestureEnabled(true)
                    }
                    binding.templateEditorView.drawView?.setDrawingEnabled(true)
                    binding.templateEditorView.drawView?.setGestureEnabled(true)
                    syncUndoRedoUI()
                }
                3 -> {
                    binding.llBg.visibility = View.VISIBLE
                    binding.listBg.layoutManager =
                        LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                    binding.listBg.adapter = BackgroundAdapter(this, this)
                    binding.icCheckBackground.setOnClickListener {
                        binding.llBg.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }
                }
                4 -> {
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
                        binding.templateEditorView.drawView?.setGestureEnabled(true)
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
                val space = progress.toFloat() * 0.5f
                binding.templateEditorView.setCellSpacing(space)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.seekbarCorner.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvCorner.text = progress.toString()
                val corner = progress.toFloat() * 1.5f
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
        if (beardAdapter != null) return
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
    // Template loading — dùng aspect ratio thật của bitmap
    // ──────────────────────────────────────────────────────

    private fun loadTemplate() {
        if (templateLoaded) return

        binding.templateEditorView.viewTreeObserver.addOnGlobalLayoutListener(
            object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val viewW = binding.templateEditorView.width
                    val viewH = binding.templateEditorView.height

                    if (viewW <= 0 || viewH <= 0) return

                    binding.templateEditorView.viewTreeObserver
                        .removeOnGlobalLayoutListener(this)

                    if (templateLoaded) return
                    templateLoaded = true

                    Log.d("TemplateEditor", "Loading: viewW=$viewW viewH=$viewH")

                    lifecycleScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            // Đọc kích thước gốc trước
                            val opts = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                            BitmapFactory.decodeResource(resources, templateData.drawableRes, opts)
                            val origW = opts.outWidth
                            val origH = opts.outHeight

                            Log.d("TemplateEditor",
                                "Template ${templateData.id} original: ${origW}x${origH}")

                            // Sample size để giảm RAM
                            var sampleSize = 1
                            while (origW / sampleSize > 1500) {
                                sampleSize *= 2
                            }
                            opts.inJustDecodeBounds = false
                            opts.inSampleSize = sampleSize

                            val rawDecoded = BitmapFactory.decodeResource(
                                resources, templateData.drawableRes, opts
                            )

                            // Scale về width = TEMPLATE_W (1125), height giữ aspect ratio
                            val targetW = TEMPLATE_W.toInt()
                            val targetH = (TEMPLATE_W * rawDecoded.height / rawDecoded.width).toInt()

                            val rawBmp = if (rawDecoded.width != targetW) {
                                val scaled = rawDecoded.scale(targetW, targetH)
                                if (scaled != rawDecoded) rawDecoded.recycle()
                                scaled
                            } else {
                                rawDecoded
                            }

                            val maskBmp = if (templateData.maskMode == MaskMode.BLACK)
                                binding.templateEditorView.createMaskFromBlack(rawBmp)
                            else
                                binding.templateEditorView.createMaskFromWhite(rawBmp)

                            TemplateLoadResult(rawBmp, maskBmp, targetW.toFloat(), targetH.toFloat())
                        }

                        Log.d("TemplateEditor",
                            "Final bitmap: ${result.raw.width}x${result.raw.height}, " +
                                    "logic: ${result.logicW}x${result.logicH}")

                        // Set logic space theo aspect ratio thật của bitmap
                        binding.templateEditorView.templateLogicW = result.logicW
                        binding.templateEditorView.templateLogicH = result.logicH

                        binding.templateEditorView.templateBitmapRaw  = result.raw
                        binding.templateEditorView.templateMaskBitmap = result.mask

                        // Cells dùng tọa độ logic gốc
                        binding.templateEditorView.cells = templateData.cellRects.map { rect ->
                            PhotoCell(RectF(rect))
                        }.toMutableList()

                        // Trigger remeasure để view tự co lại đúng aspect ratio
                        binding.templateEditorView.requestLayout()
                        binding.templateEditorView.invalidate()
                    }
                }
            }
        )
    }

    // Helper class để return 4 giá trị
    // Helper class
    private data class TemplateLoadResult(
        val raw: Bitmap,
        val mask: Bitmap,
        val logicW: Float,
        val logicH: Float
    )

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
            val savedUri = withContext(Dispatchers.IO) {
                runCatching { saveToGallery(binding.templateEditorView.export()) }.getOrNull()
            }
            if (savedUri != null) {
                startActivity(
                    Intent(this@TemplateEditorActivity, ShowImageActivity::class.java).apply {
                        putExtra("image_uri", savedUri.toString())
                    }
                )
            } else {
                Toast.makeText(this@TemplateEditorActivity, "❌ Save failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveToGallery(bitmap: Bitmap): Uri {
        val filename = "collage_${System.currentTimeMillis()}.jpg"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            uri
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
            Uri.fromFile(file)
        }
    }

    private fun dipToPx(dip: Int): Int =
        (dip * resources.displayMetrics.density).toInt()
}