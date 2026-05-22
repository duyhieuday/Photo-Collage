package com.example.piceditor.templates_editor

import android.app.Dialog
import android.content.Intent
import android.content.ContentValues
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IntDef
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piceditor.R
import com.example.piceditor.adapters.BackgroundAdapter
import com.example.piceditor.adapters.ColorAdapter
import com.example.piceditor.adapters.FontAdapter
import com.example.piceditor.adapters.FontItem
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
import java.io.File
import java.io.FileOutputStream
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.example.piceditor.ShowImageActivity
import androidx.core.graphics.scale
import com.example.piceditor.ads.InterAds
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropFragment
import com.yalantis.ucrop.UCropFragmentCallback
import com.yalantis.ucrop.view.GestureCropImageView
import androidx.core.graphics.createBitmap
import androidx.core.view.isVisible

class TemplateEditorActivity : BaseActivityNew<ActivityTemplateEditorBinding>(),
    BackgroundAdapter.OnBGClickListener,
    DrawInteractListener,
    UCropFragmentCallback {

    companion object {
        const val EXTRA_TEMPLATE_ID = "extra_template_id"
        const val TYPE_GESTURE = 0
        const val TYPE_SHAPE   = 1
        const val TYPE_ERASER  = 2
        private const val CROP_FRAGMENT_TAG = "ucrop_fragment"
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(TYPE_GESTURE, TYPE_SHAPE, TYPE_ERASER)
    annotation class DrawType

    // -- Template state --
    private var selectedCell: PhotoCell? = null
    private lateinit var templateData: TemplateData

    private val TEMPLATE_W = 1125f

    // -- Draw / sticker state --
    @DrawType private var drawType = TYPE_GESTURE
    private var drawColor: Int = Color.BLACK
    private var gestureSize: Float = 16f
    private var eraserSize: Float  = 20f
    private var gesturePaintStyle: PaintStyle = PaintStyle.STROKE

    // -- Draw panel init flag --
    private var drawPanelInitialized = false

    // -- Sticker --
    private var beardAdapter: BeardAdapter? = null

    // -- Background --
    private var backgroundBitmap: Bitmap? = null

    // -- Template loading flag --
    private var templateLoaded = false

    // -- Transform state --
    private var outputRotation = 0f
    private var outputFlipH = false
    private var outputFlipV = false

    // -- Crop --
    private var cropFragment: UCropFragment? = null
    private var cropDestUri: Uri? = null

    // -- Gallery launcher --
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            loadImageFromUri(uri)
        }

    // -- BaseActivityNew overrides --
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

    override fun onBackPressed() {
        if (binding.flCropContainer.isVisible) {
            closeCropOverlay()
            return
        }
        super.onBackPressed()
        InterAds.showAdsBreak(this@TemplateEditorActivity) { finish() }
    }

    override fun afterSetContentView() {
        super.afterSetContentView()
        BarsUtils.setHideNavigation(this)
        BarsUtils.setStatusBarColor(this, "#01000000".toColorInt())
        BarsUtils.setAppearanceLightStatusBars(this, true)
    }

    // -- Lifecycle --

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val templateId = intent.getStringExtra(EXTRA_TEMPLATE_ID)
            ?: TemplateRepository.all.first().id
        templateData = TemplateRepository.findById(templateId)
            ?: TemplateRepository.all.first()

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

    // -- Undo / Redo --

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

    // -- Background callback --

    override fun onBGClick(drawable: Drawable) {
        val bitmap = (drawable as BitmapDrawable).bitmap
        backgroundBitmap = bitmap
        binding.templateEditorView.setBackgroundBitmap(bitmap)
    }

    // -- Setup --

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
            ToolItem(R.drawable.ic_draw,         getString(R.string.draw)),
            ToolItem(R.drawable.ic_text,         getString(R.string.text)),
            ToolItem(R.drawable.ic_transform,    getString(R.string.transform)),
            ToolItem(R.drawable.ic_crop,         getString(R.string.crop)),
        )

        val adapter = ToolAdapter(tools) { _, pos ->
            binding.llPhoto.visibility     = View.GONE
            binding.llBorder.visibility    = View.GONE
            binding.llSticker.visibility   = View.GONE
            binding.llBg.visibility        = View.GONE
            binding.llDraw.visibility      = View.GONE
            binding.llTransform.visibility = View.GONE
            binding.rcvTools.visibility    = View.GONE
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
                4 -> { // Draw
                    binding.llDraw.visibility = View.VISIBLE
                    setupDrawPanel()

                    binding.icBrush.setOnClickListener {
                        binding.icBrush.setBackgroundResource(R.drawable.bg_icon_draw)
                        binding.icErase.setBackgroundResource(0)
                        drawType = TYPE_GESTURE
                        binding.seekbarBrushSize.progress = gestureSize.toInt()
                        binding.tvBrushSize.text = gestureSize.toInt().toString()
                        updateDraw()
                    }
                    binding.icErase.setOnClickListener {
                        binding.icErase.setBackgroundResource(R.drawable.bg_icon_draw)
                        binding.icBrush.setBackgroundResource(0)
                        drawType = TYPE_ERASER
                        binding.seekbarBrushSize.progress = eraserSize.toInt()
                        binding.tvBrushSize.text = eraserSize.toInt().toString()
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
                5 -> { // Text
                    binding.rcvTools.visibility = View.VISIBLE
                    binding.templateEditorView.drawView?.setDrawingEnabled(false)
                    showAddTextDialog()
                }
                6 -> { // Transform
                    binding.llTransform.visibility = View.VISIBLE
                    binding.btnRotateLeft.setOnClickListener {
                        outputRotation = (outputRotation + 90f) % 360f
                        applyContainerTransform()
                    }
                    binding.btnFlipH.setOnClickListener {
                        outputFlipH = !outputFlipH
                        applyContainerTransform()
                    }
                    binding.btnFlipV.setOnClickListener {
                        outputFlipV = !outputFlipV
                        applyContainerTransform()
                    }
                    binding.icCheckTransform.setOnClickListener {
                        binding.llTransform.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }
                }
                7 -> { // Crop
                    binding.rcvTools.visibility = View.VISIBLE
                    openCropOverlay()
                }
            }
        }

        binding.rcvTools.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvTools.adapter = adapter
    }

    // -- Border seekbars --

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

    // -- Draw: SeekBar size + dai mau --

    private fun setupDrawPanel() {
        if (drawPanelInitialized) return
        drawPanelInitialized = true

        binding.seekbarBrushSize.progress = gestureSize.toInt()
        binding.tvBrushSize.text = gestureSize.toInt().toString()
        binding.seekbarBrushSize.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val size = progress.coerceAtLeast(2).toFloat()
                    if (drawType == TYPE_GESTURE) {
                        gestureSize = size
                    } else {
                        eraserSize = size
                    }
                    binding.tvBrushSize.text = size.toInt().toString()
                    updateDraw()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            }
        )

        binding.listBrushColor.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.listBrushColor.adapter = ColorAdapter { c ->
            drawColor = c
            updateDraw()
        }
    }

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

    // -- Add Text --

    private fun showAddTextDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_input_text, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val edtText  = view.findViewById<EditText>(R.id.dlg_edit_text)
        val tvCancel = view.findViewById<AppCompatTextView>(R.id.dlg_cancel)
        val tvDone   = view.findViewById<AppCompatTextView>(R.id.dlg_done)
        val listColor = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.dlg_list_color)
        val listFont  = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.dlg_list_font)

        var pickedColor = Color.BLACK
        var pickedTypeface: Typeface? = null

        listColor.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        listColor.adapter = ColorAdapter { c ->
            pickedColor = c
            edtText.setTextColor(c)
        }

        // Font -- sua cho khop font that trong res/font/
        val fonts = listOf(
            FontItem("Default", R.font.geistmono_regular),
            FontItem("Bold",    R.font.handlee_regular),
            FontItem("Poppins", R.font.herdrock),
            FontItem("Semi",    R.font.holtwoodonesc_regular),
            FontItem("Default", R.font.imperialscript_regular),
            FontItem("Bold",    R.font.indieflower_regular),
            FontItem("Poppins", R.font.inter_18pt_medium),
            FontItem("Semi",    R.font.jersey15_regular),
            FontItem("Poppins", R.font.kiss_boom),
            FontItem("Semi",    R.font.limelight_regular),
        )
        val fontAdapter = FontAdapter(this, fonts) { tf ->
            pickedTypeface = tf
            edtText.typeface = tf
        }
        listFont.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        listFont.adapter = fontAdapter
        pickedTypeface = fontAdapter.getSelectedTypeface()

        tvCancel.setOnClickListener { dialog.dismiss() }

        tvDone.setOnClickListener {
            val text = edtText.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_enter_the_content), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            addTextAsSticker(text, pickedColor, pickedTypeface)
        }

        dialog.show()
    }

    private fun addTextAsSticker(text: String, textColor: Int, typeface: Typeface?) {
        try {
            val bitmap = renderTextToBitmap(text, textColor, typeface)
            val file = File(cacheDir, "text_sticker_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            binding.templateEditorView.drawView?.drawManager
                ?.addSticker(StickerData(file.absolutePath))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Khong tao duoc text", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderTextToBitmap(text: String, textColor: Int, typeface: Typeface?): Bitmap {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = textColor
            this.typeface = typeface ?: Typeface.DEFAULT
            textSize = 120f
        }

        val padding = 40
        val maxLineWidth = text.split("\n")
            .maxOf { textPaint.measureText(it) }
            .toInt()
        val layoutWidth = maxLineWidth.coerceAtLeast(1)

        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, layoutWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        val bmpW = layoutWidth + padding * 2
        val bmpH = staticLayout.height + padding * 2

        val bitmap = createBitmap(bmpW, bmpH)
        val canvas = Canvas(bitmap)
        canvas.translate(padding.toFloat(), padding.toFloat())
        staticLayout.draw(canvas)

        return bitmap
    }

    // -- Transform --

    private fun applyContainerTransform() {
        val view = binding.templateEditorView
        val parentW = (view.parent as? View)?.width ?: view.width
        val parentH = (view.parent as? View)?.height ?: view.height
        if (view.width == 0 || view.height == 0) return

        view.rotation = outputRotation
        view.scaleX = if (outputFlipH) -1f else 1f
        view.scaleY = if (outputFlipV) -1f else 1f

        val rotated90 = (outputRotation % 180f) != 0f
        if (rotated90 && parentW > 0 && parentH > 0) {
            val fitScale = minOf(
                parentW.toFloat() / view.height,
                parentH.toFloat() / view.width
            ).coerceAtMost(1f)
            view.scaleX *= fitScale
            view.scaleY *= fitScale
        }
    }

    private fun applyTransform(src: Bitmap): Bitmap {
        if (outputRotation == 0f && !outputFlipH && !outputFlipV) return src
        val matrix = Matrix()
        if (outputFlipH || outputFlipV) {
            matrix.postScale(
                if (outputFlipH) -1f else 1f,
                if (outputFlipV) -1f else 1f,
                src.width / 2f, src.height / 2f
            )
        }
        if (outputRotation != 0f) {
            matrix.postRotate(outputRotation, src.width / 2f, src.height / 2f)
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    // -- Crop --

    private fun openCropOverlay() {
        val bmp = try {
            applyTransform(binding.templateEditorView.export())
        } catch (e: Throwable) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.unable_to_read_image_file), Toast.LENGTH_SHORT).show()
            return
        }

        val srcFile = File(cacheDir, "crop_src_${System.currentTimeMillis()}.jpg")
        try {
            FileOutputStream(srcFile).use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.image_not_ready), Toast.LENGTH_SHORT).show()
            return
        }

        val srcUri = Uri.fromFile(srcFile)
        val destFile = File(cacheDir, "crop_dest_${System.currentTimeMillis()}.jpg")
        cropDestUri = Uri.fromFile(destFile)

        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(95)
            setHideBottomControls(true)
            setFreeStyleCropEnabled(true)
            setShowCropGrid(true)
            setShowCropFrame(true)
            setActiveControlsWidgetColor("#039855".toColorInt())
            setRootViewBackgroundColor("#FFFFFF".toColorInt())
            setCropFrameColor("#039855".toColorInt())
            setCropGridColor("#80FFFFFF".toColorInt())
            setDimmedLayerColor("#99000000".toColorInt())
        }

        val uCrop = UCrop.of(srcUri, cropDestUri!!).withOptions(options)
        val fragment = uCrop.getFragment(uCrop.getIntent(this).extras!!)
        cropFragment = fragment

        supportFragmentManager.beginTransaction()
            .replace(R.id.crop_fragment_container, fragment, CROP_FRAGMENT_TAG)
            .commitAllowingStateLoss()

        binding.flCropContainer.visibility = View.VISIBLE

        binding.cropBtnCancel.setOnClickListener { closeCropOverlay() }
        binding.cropBtnDone.setOnClickListener { cropFragment?.cropAndSaveImage() }
        binding.cropBtnReset.setOnClickListener { resetCrop() }

        binding.ratioFree.setOnClickListener { selectRatio(it, 0f, 0f) }
        binding.ratio11.setOnClickListener  { selectRatio(it, 1f, 1f) }
        binding.ratio45.setOnClickListener  { selectRatio(it, 4f, 5f) }
        binding.ratio54.setOnClickListener  { selectRatio(it, 5f, 4f) }
        binding.ratio23.setOnClickListener  { selectRatio(it, 2f, 3f) }
        binding.ratio916.setOnClickListener { selectRatio(it, 9f, 16f) }
        binding.ratio169.setOnClickListener { selectRatio(it, 16f, 9f) }
        binding.ratio12.setOnClickListener  { selectRatio(it, 1f, 2f) }

        binding.flCropContainer.post {
            selectRatio(binding.ratioFree, 0f, 0f)
        }
    }

    private fun selectRatio(view: View, ratioX: Float, ratioY: Float) {
        binding.ratioFree.isSelected = false
        binding.ratio11.isSelected   = false
        binding.ratio45.isSelected   = false
        binding.ratio54.isSelected   = false
        binding.ratio23.isSelected   = false
        binding.ratio916.isSelected  = false
        binding.ratio169.isSelected  = false
        binding.ratio12.isSelected   = false
        view.isSelected = true

        val cropImageView = getCropImageView() ?: return
        if (ratioX == 0f || ratioY == 0f) {
            cropImageView.targetAspectRatio = 0f
        } else {
            cropImageView.targetAspectRatio = ratioX / ratioY
        }
        cropImageView.setImageToWrapCropBounds()
    }

    private fun resetCrop() {
        val cropImageView = getCropImageView() ?: return
        cropImageView.targetAspectRatio = 0f
        cropImageView.setImageToWrapCropBounds()
        selectRatio(binding.ratioFree, 0f, 0f)
    }

    private fun getCropImageView(): GestureCropImageView? {
        val fragment = cropFragment ?: return null
        return try {
            val field = UCropFragment::class.java.getDeclaredField("mGestureCropImageView")
            field.isAccessible = true
            field.get(fragment) as? GestureCropImageView
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun closeCropOverlay() {
        binding.flCropContainer.visibility = View.GONE
        cropFragment?.let {
            supportFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
        }
        cropFragment = null
    }

    // -- UCropFragmentCallback --
    override fun loadingProgress(showLoader: Boolean) {}

    override fun onCropFinish(result: UCropFragment.UCropResult?) {
        if (result == null) { closeCropOverlay(); return }
        if (result.mResultCode == RESULT_OK) {
            val uri = result.mResultData?.let { UCrop.getOutput(it) } ?: cropDestUri
            closeCropOverlay()
            if (uri != null) {
                InterAds.showAdsBreak(this@TemplateEditorActivity) {
                    startActivity(Intent(this, ShowImageActivity::class.java).apply {
                        putExtra("image_uri", uri.toString())
                    })
                    finish()
                }
            }
        } else if (result.mResultCode == UCrop.RESULT_ERROR) {
            result.mResultData?.let { UCrop.getError(it) }?.printStackTrace()
            Toast.makeText(this, "Crop that bai", Toast.LENGTH_SHORT).show()
            closeCropOverlay()
        }
    }

    // -- Photo cell list --

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

    // -- Stickers --

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

    // -- Template loading --

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

                    lifecycleScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            val opts = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                            BitmapFactory.decodeResource(resources, templateData.drawableRes, opts)
                            val origW = opts.outWidth
                            opts.outHeight
                            var sampleSize = 1
                            while (origW / sampleSize > 1500) {
                                sampleSize *= 2
                            }
                            opts.inJustDecodeBounds = false
                            opts.inSampleSize = sampleSize

                            val rawDecoded = BitmapFactory.decodeResource(
                                resources, templateData.drawableRes, opts
                            )

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

                        binding.templateEditorView.templateLogicW = result.logicW
                        binding.templateEditorView.templateLogicH = result.logicH

                        binding.templateEditorView.templateBitmapRaw  = result.raw
                        binding.templateEditorView.templateMaskBitmap = result.mask

                        binding.templateEditorView.cells = templateData.cellRects.map { rect ->
                            PhotoCell(RectF(rect))
                        }.toMutableList()

                        binding.templateEditorView.requestLayout()
                        binding.templateEditorView.invalidate()
                    }
                }
            }
        )
    }

    private data class TemplateLoadResult(
        val raw: Bitmap,
        val mask: Bitmap,
        val logicW: Float,
        val logicH: Float
    )

    // -- Gallery --

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
                Toast.makeText(this@TemplateEditorActivity, getString(R.string.unable_to_read_image_file), Toast.LENGTH_SHORT).show()
                return@launch
            }
            selectedCell?.let { binding.templateEditorView.setImageToCell(it, bitmap) }
                ?: Toast.makeText(this@TemplateEditorActivity, getString(R.string.the_image_box), Toast.LENGTH_SHORT).show()
        }
    }

    // -- Export --

    private fun onExportClick() {
        lifecycleScope.launch {
            val savedUri = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = binding.templateEditorView.export()
                    saveToGallery(applyTransform(raw))
                }.getOrNull()
            }
            if (savedUri != null) {
                startActivity(
                    Intent(this@TemplateEditorActivity, ShowImageActivity::class.java).apply {
                        putExtra("image_uri", savedUri.toString())
                    }
                )
            } else {
                Toast.makeText(this@TemplateEditorActivity, "Save failed", Toast.LENGTH_SHORT).show()
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

}