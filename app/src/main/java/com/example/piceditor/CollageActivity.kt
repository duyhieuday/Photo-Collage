package com.example.piceditor

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piceditor.adapters.BackgroundAdapter
import com.example.piceditor.adapters.ColorAdapter
import com.example.piceditor.adapters.FontAdapter
import com.example.piceditor.adapters.FontItem
import com.example.piceditor.adapters.FrameAdapter
import com.example.piceditor.adapters.ToolAdapter
import com.example.piceditor.ads.InterAds
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityCollageBinding
import com.example.piceditor.draw.DrawInteractListener
import com.example.piceditor.draw.DrawerManager
import com.example.piceditor.draw.model.draw.DrawPath
import com.example.piceditor.draw.model.draw.style.BrushStyle
import com.example.piceditor.draw.model.draw.style.PaintStyle
import com.example.piceditor.draw.model.sticker.StickerData
import com.example.piceditor.draw.test.Beard
import com.example.piceditor.draw.test.BeardAdapter
import com.example.piceditor.frame.FramePhotoLayout
import com.example.piceditor.model.TemplateItem
import com.example.piceditor.model.ToolItem
import com.example.piceditor.utils.AndroidUtils
import com.example.piceditor.utils.BarsUtils
import com.example.piceditor.utils.FrameImageUtils
import com.example.piceditor.utils.ImageUtils
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropFragment
import com.yalantis.ucrop.UCropFragmentCallback
import com.yalantis.ucrop.view.GestureCropImageView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import androidx.core.view.isVisible

open class CollageActivity : BaseActivityNew<ActivityCollageBinding>(), View.OnClickListener,
    FrameAdapter.OnFrameClickListener, BackgroundAdapter.OnBGClickListener, DrawInteractListener,
    UCropFragmentCallback {

    var mFramePhotoLayout: FramePhotoLayout? = null
    var DEFAULT_SPACE: Float = 0.0f
    var MAX_SPACE: Float = 0.0f
    var MAX_CORNER: Float = 0.0f
    private val RATIO_SQUARE = 0
    private val RATIO_GOLDEN = 2
    private var mSpace = DEFAULT_SPACE
    private var mCorner = 0f
    val MAX_SPACE_PROGRESS = 300.0f
    val MAX_CORNER_PROGRESS = 200.0f
    private var mBackgroundColor = Color.WHITE
    private var mBackgroundImage: Bitmap? = null
    private var mSavedInstanceState: Bundle? = null
    private var mLayoutRatio = RATIO_SQUARE
    protected var mOutputScale = 1f
    protected var mSelectedTemplateItem: TemplateItem? = null
    private var mImageInTemplateCount = 0
    private var mTemplateItemList: ArrayList<TemplateItem>? = ArrayList()
    private var mSelectedPhotoPaths: MutableList<String> = java.util.ArrayList()
    lateinit var frameAdapter: FrameAdapter
    private var mLastClickTime: Long = 0
    private var drawerManager: DrawerManager? = null
    private var beardAdapter: BeardAdapter? = null
    private var beardList: MutableList<Beard?>? = null
    private var stickerPanelController: com.example.piceditor.sticker.StickerPanelController? = null

    companion object {
        const val TYPE_GESTURE = 0
        const val TYPE_SHAPE = 1
        const val TYPE_ERASER = 2
        private const val CROP_FRAGMENT_TAG = "ucrop_fragment"
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(TYPE_GESTURE, TYPE_SHAPE, TYPE_ERASER)
    annotation class Type

    @Type
    private var type = TYPE_GESTURE
    private var color: Int = Color.BLACK
    private var gestureSize: Float = 16f
    private var shapeSize: Float = 20f
    private var eraserSize: Float = 20f
    private var gesturePaintStyle: PaintStyle = PaintStyle.STROKE
    private var shapePaintStyle: PaintStyle = PaintStyle.STROKE
    private var shapeBrushStyle: BrushStyle = BrushStyle.HEART

    // ── Transform state (áp lúc export) ──
    private var outputRotation = 0f
    private var outputFlipH = false
    private var outputFlipV = false

    // ── Crop ──
    private var cropFragment: UCropFragment? = null
    private var cropDestUri: Uri? = null

    // ── Draw panel init flag ──
    private var drawPanelInitialized = false

    fun getDrawerManager(): DrawerManager? {
        if (drawerManager == null) {
            drawerManager = binding.drawView.drawManager
        }
        return drawerManager
    }

    private fun checkClick() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return
        mLastClickTime = SystemClock.elapsedRealtime()
    }

    private fun applyContainerTransform() {
        val container = binding.rlContainer
        val parentW = (container.parent as? View)?.width ?: container.width
        val parentH = (container.parent as? View)?.height ?: container.height
        if (container.width == 0 || container.height == 0) return

        container.rotation = outputRotation
        container.scaleX = if (outputFlipH) -1f else 1f
        container.scaleY = if (outputFlipV) -1f else 1f

        // Bù scale khi xoay 90/270 để vừa khung cha
        val rotated90 = (outputRotation % 180f) != 0f
        if (rotated90 && parentW > 0 && parentH > 0) {
            val fitScale = minOf(
                parentW.toFloat() / container.height,
                parentH.toFloat() / container.width
            ).coerceAtMost(1f)
            container.scaleX *= fitScale
            container.scaleY *= fitScale
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Đồng bộ visual undo/redo
    // ─────────────────────────────────────────────────────────────────────────
    private fun syncUndoRedoUI() {
        val manager = getDrawerManager() ?: return
        val canUndo = manager.isActiveUndo
        val canRedo = manager.isActiveRedo

        binding.btnUndo.isEnabled = canUndo
        binding.btnUndo.alpha     = if (canUndo) 1f else 0.3f
        binding.btnUndo.colorFilter = if (canUndo) null
        else ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })

        binding.btnRedo.isEnabled = canRedo
        binding.btnRedo.alpha     = if (canRedo) 1f else 0.3f
        binding.btnRedo.colorFilter = if (canRedo) null
        else ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DrawInteractListener callbacks
    // ─────────────────────────────────────────────────────────────────────────

    override fun interactUndoRedoChange() {
        syncUndoRedoUI()
    }

    override fun interactStickerFocusChange(stickerData: StickerData?) {}
    override fun interactTouchDown() {}
    override fun interactTouchUp() {}
    override fun interactUpdateBackground(url: String?) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Adapter callbacks
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressLint("SuspiciousIndentation")
    override fun onBGClick(drawable: Drawable) {
        val bmp = mFramePhotoLayout!!.createImage()
        val bitmap = (drawable as BitmapDrawable).bitmap
        mBackgroundImage = AndroidUtils.resizeImageToNewSize(bitmap, bmp.width, bmp.height)
        binding.imgBackground.setImageBitmap(mBackgroundImage)
    }

    override fun onFrameClick(templateItem: TemplateItem) {
        mSelectedTemplateItem!!.isSelected = false
        for (idx in 0 until mSelectedTemplateItem!!.photoItemList.size) {
            val photoItem = mSelectedTemplateItem!!.photoItemList[idx]
            if (photoItem.imagePath != null && photoItem.imagePath!!.length > 0) {
                if (idx < mSelectedPhotoPaths.size) mSelectedPhotoPaths.add(idx, photoItem.imagePath!!)
                else mSelectedPhotoPaths.add(photoItem.imagePath!!)
            }
        }
        val size = Math.min(mSelectedPhotoPaths.size, templateItem.photoItemList.size)
        for (idx in 0 until size) {
            val photoItem = templateItem.photoItemList[idx]
            if (photoItem.imagePath == null || photoItem.imagePath!!.length < 1) {
                photoItem.imagePath = mSelectedPhotoPaths[idx]
            }
        }
        mSelectedTemplateItem = templateItem
        mSelectedTemplateItem!!.isSelected = true
        frameAdapter.notifyDataSetChanged()
        buildLayout(templateItem)
    }

    inner class space_listener : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            mSpace = MAX_SPACE * progress / MAX_SPACE_PROGRESS
            mFramePhotoLayout?.setSpace(mSpace, mCorner)
            binding.tvGrid.text = progress.toString()
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    inner class corner_listener : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            mCorner = MAX_CORNER * progress / MAX_CORNER_PROGRESS
            mFramePhotoLayout?.setSpace(mSpace, mCorner)
            binding.tvCorner.text = progress.toString()
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btnNext -> {
                checkClick()
                InterAds.showAdsBreak(this@CollageActivity) {
                    val bitmap = createOutputImage()
                    val uri = saveTempBitmap(bitmap)
                    val intent = Intent(this, FilterCollageActivity::class.java)
                    intent.putExtra("image_uri", uri.toString())
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    override fun getLayoutRes(): Int = R.layout.activity_collage
    override fun getFrame(): Int = 0
    override fun getDataFromIntent() {}

    override fun doAfterOnCreate() {
        if (PreferenceUtil.getInstance(this).getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")) {
            initBanner(binding.banner.adViewContainer)
        } else {
            initBanner(binding.adViewContainer)
            binding.banner.getRoot().visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (!PreferenceUtil.getInstance(this).getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")) {
            initBanner(binding.adViewContainer)
            binding.banner.getRoot().visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        if (binding.flCropContainer.isVisible) {
            closeCropOverlay()
            return
        }
        super.onBackPressed()
        InterAds.showAdsBreak(this@CollageActivity) { finish() }
    }

    override fun setListener() {}
    override fun initFragment(): BaseFragment<*>? = null

    override fun afterSetContentView() {
        super.afterSetContentView()
        BarsUtils.setHideNavigation(this)
        BarsUtils.setStatusBarColor(this, "#01000000".toColorInt())
        BarsUtils.setAppearanceLightStatusBars(this, true)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnBack.setOnClickListener {
            InterAds.showAdsBreak(this@CollageActivity) { finish() }
        }

        setUpTab()

        binding.drawView.setDrawingEnabled(false)

        DEFAULT_SPACE = ImageUtils.pxFromDp(this, 2F)
        MAX_SPACE     = ImageUtils.pxFromDp(this, 30F)
        MAX_CORNER    = ImageUtils.pxFromDp(this, 60F)
        mSpace = DEFAULT_SPACE

        if (savedInstanceState != null) {
            mSpace  = savedInstanceState.getFloat("mSpace")
            mCorner = savedInstanceState.getFloat("mCorner")
            mSavedInstanceState = savedInstanceState
        }

        mImageInTemplateCount = intent.getIntExtra("imagesinTemplate", 0)
        val extraImagePaths   = intent.getStringArrayListExtra("selectedImages")

        binding.listBg.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.listBg.adapter = BackgroundAdapter(this, this)

        setupStickerPanel()

        binding.seekbarSpace.setOnSeekBarChangeListener(space_listener())
        binding.seekbarCorner.setOnSeekBarChangeListener(corner_listener())

        binding.rlContainer.viewTreeObserver
            .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mOutputScale = ImageUtils.calculateOutputScaleFactor(
                        binding.rlContainer.width, binding.rlContainer.height
                    )
                    buildLayout(mSelectedTemplateItem!!)
                    binding.rlContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })

        loadFrameImages()
        binding.listFrames.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        frameAdapter = FrameAdapter(this, mTemplateItemList!!, this)
        binding.listFrames.adapter = frameAdapter

        mSelectedTemplateItem = mTemplateItemList!!.get(0)
        mSelectedTemplateItem!!.isSelected = true

        if (extraImagePaths != null) {
            val size = extraImagePaths.size.coerceAtMost(mSelectedTemplateItem!!.photoItemList.size)
            for (i in 0 until size)
                mSelectedTemplateItem!!.photoItemList[i].imagePath = extraImagePaths[i]
        }

        binding.btnNext.setOnClickListener(this)

        binding.drawView.drawManager.addDrawInteractListener(this)

        syncUndoRedoUI()

        binding.btnUndo.setOnClickListener {
            getDrawerManager()?.undo()
        }

        binding.btnRedo.setOnClickListener {
            getDrawerManager()?.redo()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Add Text — nhập text, chọn font + màu, render thành sticker ảnh
    // ─────────────────────────────────────────────────────────────────────────

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

        // Font — sửa cho khớp font thật trong res/font/
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
            getDrawerManager()?.addSticker(StickerData(file.absolutePath))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.unable_to_create_text), Toast.LENGTH_SHORT).show()
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

    // ─────────────────────────────────────────────────────────────────────────
    // Setup SeekBar size + dãy màu cho tab Draw
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupDrawPanel() {
        if (drawPanelInitialized) return
        drawPanelInitialized = true

        binding.seekbarBrushSize.progress = gestureSize.toInt()
        binding.tvBrushSize.text = gestureSize.toInt().toString()
        binding.seekbarBrushSize.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val size = progress.coerceAtLeast(2).toFloat()
                    if (type == TYPE_GESTURE) {
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
            color = c
            updateDraw()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Transform — áp rotate/flip lên bitmap output
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // Crop — render cả tấm → uCrop overlay → sang FilterCollageActivity
    // ─────────────────────────────────────────────────────────────────────────

    private fun openCropOverlay() {
        val bmp = try {
            createOutputImage()
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
            Toast.makeText(this, getString(R.string.unable_to_read_image_file), Toast.LENGTH_SHORT).show()
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

        // Các nút tỉ lệ
        binding.ratioFree.setOnClickListener { selectRatio(it, 0f, 0f) }
        binding.ratio11.setOnClickListener  { selectRatio(it, 1f, 1f) }
        binding.ratio45.setOnClickListener  { selectRatio(it, 4f, 5f) }
        binding.ratio54.setOnClickListener  { selectRatio(it, 5f, 4f) }
        binding.ratio23.setOnClickListener  { selectRatio(it, 2f, 3f) }
        binding.ratio916.setOnClickListener { selectRatio(it, 9f, 16f) }
        binding.ratio169.setOnClickListener { selectRatio(it, 16f, 9f) }
        binding.ratio12.setOnClickListener  { selectRatio(it, 1f, 2f) }

        // Mặc định chọn Free sau khi fragment gắn xong
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

    // ── UCropFragmentCallback ──
    override fun loadingProgress(showLoader: Boolean) {}

    override fun onCropFinish(result: UCropFragment.UCropResult?) {
        if (result == null) { closeCropOverlay(); return }
        if (result.mResultCode == RESULT_OK) {
            val uri = result.mResultData?.let { UCrop.getOutput(it) } ?: cropDestUri
            closeCropOverlay()
            if (uri != null) {
                InterAds.showAdsBreak(this@CollageActivity) {
                    startActivity(Intent(this, FilterCollageActivity::class.java).apply {
                        putExtra("image_uri", uri.toString())
                    })
                    finish()
                }
            }
        } else if (result.mResultCode == UCrop.RESULT_ERROR) {
            result.mResultData?.let { UCrop.getError(it) }?.printStackTrace()
            Toast.makeText(this, getString(R.string.crop_failed), Toast.LENGTH_SHORT).show()
            closeCropOverlay()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveTempBitmap(bitmap: Bitmap): Uri {
        val file = File(cacheDir, "collage_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return FileProvider.getUriForFile(this, "${packageName}.provider", file)
    }

    private fun setUpTab() {
        val tools = mutableListOf(
            ToolItem(R.drawable.ic_layout,     getString(R.string.layout)),
            ToolItem(R.drawable.ic_border,     getString(R.string.border)),
            ToolItem(R.drawable.ic_sticker,    getString(R.string.sticker)),
            ToolItem(R.drawable.ic_background, getString(R.string.background)),
            ToolItem(R.drawable.ic_draw,       getString(R.string.draw)),
            ToolItem(R.drawable.ic_text,       getString(R.string.text)),
            ToolItem(R.drawable.ic_transform,  getString(R.string.transform)),
            ToolItem(R.drawable.ic_crop,       getString(R.string.crop)),
        )

        val adapter = ToolAdapter(tools) { _, pos ->
            // Ẩn tất cả panel
            binding.llFrame.visibility     = View.GONE
            binding.llBorder.visibility    = View.GONE
            binding.llBg.visibility        = View.GONE
            binding.llSticker.visibility   = View.GONE
            binding.llDraw.visibility      = View.GONE
            binding.llRatio.visibility     = View.GONE
            binding.llTransform.visibility = View.GONE
            binding.rcvTools.visibility    = View.GONE

            when (pos) {
                0 -> { // Layout
                    binding.llFrame.visibility = View.VISIBLE
                    binding.icCheckLayout.setOnClickListener {
                        binding.llFrame.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }
                    binding.drawView.setDrawingEnabled(false)
                }

                1 -> { // Border
                    binding.llBorder.visibility = View.VISIBLE
                    binding.tvGrid.text   = binding.seekbarSpace.progress.toString()
                    binding.tvCorner.text = binding.seekbarCorner.progress.toString()
                    binding.icCheckBorder.setOnClickListener {
                        binding.llBorder.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }
                    binding.drawView.setDrawingEnabled(false)
                }

                2 -> { // Sticker
                    binding.llSticker.visibility = View.VISIBLE
                    binding.icCheckSticker.setOnClickListener {
                        binding.llSticker.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                        binding.drawView.setDrawingEnabled(false)
                    }
                    binding.drawView.setDrawingEnabled(true)
                    syncUndoRedoUI()
                }

                3 -> { // Background
                    binding.llBg.visibility = View.VISIBLE
                    binding.icCheckBackground.setOnClickListener {
                        binding.llBg.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }
                    binding.drawView.setDrawingEnabled(false)
                }

                4 -> { // Draw
                    binding.llDraw.visibility = View.VISIBLE
                    setupDrawPanel()

                    binding.icBrush.setOnClickListener {
                        binding.icBrush.setBackgroundResource(R.drawable.bg_icon_draw)
                        binding.icErase.setBackgroundResource(0)
                        type = TYPE_GESTURE
                        binding.seekbarBrushSize.progress = gestureSize.toInt()
                        binding.tvBrushSize.text = gestureSize.toInt().toString()
                        updateDraw()
                    }
                    binding.icErase.setOnClickListener {
                        binding.icErase.setBackgroundResource(R.drawable.bg_icon_draw)
                        binding.icBrush.setBackgroundResource(0)
                        type = TYPE_ERASER
                        binding.seekbarBrushSize.progress = eraserSize.toInt()
                        binding.tvBrushSize.text = eraserSize.toInt().toString()
                        updateDraw()
                    }
                    binding.icCheckDraw.setOnClickListener {
                        binding.llDraw.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                        binding.drawView.setDrawingEnabled(false)
                    }
                    updateDraw()
                    syncUndoRedoUI()
                }

                5 -> { // Text
                    binding.rcvTools.visibility = View.VISIBLE
                    binding.drawView.setDrawingEnabled(false)
                    showAddTextDialog()
                }

                6 -> { // Transform
                    binding.llTransform.visibility = View.VISIBLE
                    binding.drawView.setDrawingEnabled(false)

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
                    binding.drawView.setDrawingEnabled(false)
                    openCropOverlay()
                }
            }
        }

        binding.rcvTools.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvTools.adapter = adapter
    }

    private fun updateDraw() {
        val drawPath: DrawPath
        gesturePaintStyle = PaintStyle.STROKE
        shapePaintStyle   = PaintStyle.STROKE
        shapeBrushStyle   = BrushStyle.HEART

        drawPath = when (type) {
            TYPE_GESTURE -> DrawPath(BrushStyle.GESTURE, gesturePaintStyle, color, gestureSize)
            TYPE_SHAPE   -> DrawPath(shapeBrushStyle, shapePaintStyle, color, shapeSize)
            TYPE_ERASER  -> DrawPath(BrushStyle.GESTURE, PaintStyle.ERASE, color, eraserSize)
            else -> return
        }
        getDrawerManager()!!.setDrawPath(drawPath)
        binding.icBrush.isSelected = type == TYPE_GESTURE
        binding.icErase.isSelected = type == TYPE_ERASER
        binding.drawView.setDrawingEnabled(true)
    }

    private fun setupStickerPanel() {
        stickerPanelController = com.example.piceditor.sticker.StickerPanelController(
            context = this,
            categoryRecycler = binding.listStickerCategory,
            gridRecycler = binding.listSticker,
            onAddSticker = { data -> getDrawerManager()?.addSticker(data) }
        )
    }

    private fun loadFrameImages() {
        val all = java.util.ArrayList<TemplateItem>()
        all.addAll(FrameImageUtils.loadFrameImages(this))
        mTemplateItemList = java.util.ArrayList()
        if (mImageInTemplateCount > 0) {
            for (item in all)
                if (item.photoItemList.size == mImageInTemplateCount)
                    mTemplateItemList!!.add(item)
        } else {
            mTemplateItemList!!.addAll(all)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat("mSpace", mSpace)
        outState.putFloat("mCornerBar", mCorner)
        mFramePhotoLayout?.saveInstanceState(outState)
    }

    fun buildLayout(item: TemplateItem) {
        mFramePhotoLayout = FramePhotoLayout(this, item.photoItemList)

        var viewWidth  = binding.rlContainer.width
        var viewHeight = binding.rlContainer.height

        if (mLayoutRatio == RATIO_SQUARE) {
            if (viewWidth > viewHeight) viewWidth = viewHeight else viewHeight = viewWidth
        } else if (mLayoutRatio == RATIO_GOLDEN) {
            val g = 1.61803398875
            if (viewWidth <= viewHeight) {
                if (viewWidth * g >= viewHeight) viewWidth = (viewHeight / g).toInt()
                else viewHeight = (viewWidth * g).toInt()
            } else {
                if (viewHeight * g >= viewWidth) viewHeight = (viewWidth / g).toInt()
                else viewWidth = (viewHeight * g).toInt()
            }
        }

        mOutputScale = ImageUtils.calculateOutputScaleFactor(viewWidth, viewHeight)
        mFramePhotoLayout!!.build(viewWidth, viewHeight, mOutputScale, mSpace, mCorner)

        if (mSavedInstanceState != null) {
            mFramePhotoLayout!!.restoreInstanceState(mSavedInstanceState!!)
            mSavedInstanceState = null
        }

        val params = RelativeLayout.LayoutParams(viewWidth, viewHeight)
        params.addRule(RelativeLayout.CENTER_IN_PARENT)
        binding.rlContainer.removeAllViews()
        binding.rlContainer.removeView(binding.imgBackground)
        binding.rlContainer.addView(binding.imgBackground, params)
        binding.rlContainer.addView(mFramePhotoLayout, params)
        binding.rlContainer.addView(binding.drawView, params)

        binding.seekbarSpace.progress  = (MAX_SPACE_PROGRESS * mSpace / MAX_SPACE).toInt()
        binding.seekbarCorner.progress = (MAX_CORNER_PROGRESS * mCorner / MAX_CORNER).toInt()
    }

    @Throws(OutOfMemoryError::class)
    fun createOutputImage(): Bitmap {
        val template = mFramePhotoLayout!!.createImage()
        val result   = createBitmap(template.width, template.height)
        val canvas   = Canvas(result)
        val paint    = Paint(Paint.ANTI_ALIAS_FLAG)

        if (mBackgroundImage != null) {
            canvas.drawBitmap(mBackgroundImage!!, null, Rect(0, 0, result.width, result.height), paint)
        } else {
            canvas.drawColor(mBackgroundColor)
        }
        canvas.drawBitmap(template, 0f, 0f, paint)
        // Ẩn khung điều khiển (xoá, copy, scale, xoay) của sticker/text khi render ảnh lưu
        getDrawerManager()?.setStickerForceHideControls(true)
        try {
            canvas.drawBitmap(getBitmapFromView(binding.drawView), 0f, 0f, paint)
        } finally {
            getDrawerManager()?.setStickerForceHideControls(false)
        }

        return applyTransform(result)
    }

    fun getBitmapFromView(view: View): Bitmap {
        val bitmap = createBitmap(view.width, view.height)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    fun onSelectModel(s: String?) {
        getDrawerManager()!!.addSticker(StickerData(s))
    }
}