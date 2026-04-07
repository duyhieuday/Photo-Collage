package com.example.piceditor

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.annotation.IntDef
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piceditor.adapters.BackgroundAdapter
import com.example.piceditor.adapters.FrameAdapter
import com.example.piceditor.adapters.ToolAdapter
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityCollageBinding
import com.example.piceditor.draw.DrawInteractListener
import com.example.piceditor.draw.DrawerManager
import com.example.piceditor.draw.ScreenUtils
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
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

open class CollageActivity : BaseActivityNew<ActivityCollageBinding>(), View.OnClickListener,
    FrameAdapter.OnFrameClickListener, BackgroundAdapter.OnBGClickListener, DrawInteractListener {

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

    // Mode type
    companion object {
        const val TYPE_GESTURE = 0
        const val TYPE_SHAPE = 1
        const val TYPE_ERASER = 2
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

    // Gesture draw
    private var gesturePaintStyle: PaintStyle = PaintStyle.STROKE

    // Shape draw
    private var shapePaintStyle: PaintStyle = PaintStyle.STROKE
    private var shapeBrushStyle: BrushStyle = BrushStyle.HEART
    var isDrawingMode = false

    fun getDrawerManager(): DrawerManager? {
        if (drawerManager == null) {
            drawerManager = binding.drawView.drawManager
        }
        return drawerManager
    }

    private fun checkClick() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onBGClick(drawable: Drawable) {

        val bmp = mFramePhotoLayout!!.createImage()
        val bitmap = (drawable as BitmapDrawable).bitmap
        mBackgroundImage = AndroidUtils.resizeImageToNewSize(bitmap, bmp.width, bmp.height)

//        img_background.background = BitmapDrawable(resources, mBackgroundImage)
        binding.imgBackground.setImageBitmap(mBackgroundImage)

    }

    override fun onFrameClick(templateItem: TemplateItem) {

        mSelectedTemplateItem!!.isSelected = false

        for (idx in 0 until mSelectedTemplateItem!!.photoItemList.size) {
            val photoItem = mSelectedTemplateItem!!.photoItemList[idx]
            if (photoItem.imagePath != null && photoItem.imagePath!!.length > 0) {
                if (idx < mSelectedPhotoPaths.size) {
                    mSelectedPhotoPaths.add(idx, photoItem.imagePath!!)
                } else {
                    mSelectedPhotoPaths.add(photoItem.imagePath!!)
                }
            }
        }

        val size = Math.min(mSelectedPhotoPaths.size, templateItem.photoItemList.size)
        for (idx in 0 until size) {
            val photoItem = templateItem.photoItemList.get(idx)
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

            // update layout
            mFramePhotoLayout?.setSpace(mSpace, mCorner)

            // update UI
            binding.tvGrid.text = progress.toString()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    inner class corner_listener : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            mCorner = MAX_CORNER * progress / MAX_CORNER_PROGRESS

            // update layout
            mFramePhotoLayout?.setSpace(mSpace, mCorner)

            // update UI
            binding.tvCorner.text = progress.toString()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    override fun onClick(v: View?) {
        when (v!!.id) {

            R.id.btnNext -> {

                checkClick()

                val bitmap = createOutputImage()
                val uri = saveTempBitmap(bitmap)

                val intent = Intent(this, FilterCollageActivity::class.java)
                intent.putExtra("image_uri", uri.toString())
                startActivity(intent)
                finish()
            }
        }
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_collage
    }

    override fun getFrame(): Int {
        return 0
    }

    override fun getDataFromIntent() {

    }

    override fun doAfterOnCreate() {
        if (PreferenceUtil.getInstance(this).getValue(Constant.SharePrefKey.BANNER_COL, "no")
                .equals("yes")
        ) {
            initBanner(binding.banner.adViewContainer)
        } else {
            initBanner(binding.adViewContainer)
            binding.banner.getRoot().visibility = View.GONE
        }
    }

    protected override fun onResume() {
        super.onResume()
        if (PreferenceUtil.getInstance(this).getValue(Constant.SharePrefKey.BANNER_COL, "no")
                .equals("yes")
        ) {
        } else {
            initBanner(binding.adViewContainer)
            binding.banner.getRoot().visibility = View.GONE
        }
    }

    override fun setListener() {

    }

    override fun initFragment(): BaseFragment<*>? {
        return null
    }

    override fun afterSetContentView() {
        super.afterSetContentView()
        BarsUtils.setHideNavigation(this)
        BarsUtils.setStatusBarColor(this, "#01000000".toColorInt())
        BarsUtils.setAppearanceLightStatusBars(this, true)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnBack.setOnClickListener() {
            finish()
        }

        setUpTab()

        binding.drawView.setDrawingEnabled(false)

        DEFAULT_SPACE = ImageUtils.pxFromDp(this, 2F)
        MAX_SPACE = ImageUtils.pxFromDp(this, 30F)
        MAX_CORNER = ImageUtils.pxFromDp(this, 60F)
        mSpace = DEFAULT_SPACE

        if (savedInstanceState != null) {
            mSpace = savedInstanceState.getFloat("mSpace")
            mCorner = savedInstanceState.getFloat("mCorner")
            mSavedInstanceState = savedInstanceState
        }

        mImageInTemplateCount = intent.getIntExtra("imagesinTemplate", 0)
        val extraImagePaths = intent.getStringArrayListExtra("selectedImages")

        binding.listBg.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.listBg.adapter = BackgroundAdapter(this, this)

        loadImageBeards()

        binding.seekbarSpace.setOnSeekBarChangeListener(space_listener())
        binding.seekbarCorner.setOnSeekBarChangeListener(corner_listener())

        binding.rlContainer.viewTreeObserver
            .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mOutputScale = ImageUtils.calculateOutputScaleFactor(
                        binding.rlContainer.width,
                        binding.rlContainer.height
                    )
                    buildLayout(mSelectedTemplateItem!!)
                    // remove listener
                    binding.rlContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })

        loadFrameImages()
        binding.listFrames.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        frameAdapter = FrameAdapter(this, mTemplateItemList!!, this)
        binding.listFrames.adapter = frameAdapter

        mSelectedTemplateItem = mTemplateItemList!!.get(0)
        mSelectedTemplateItem!!.isSelected = true

        if (extraImagePaths != null) {
            val size =
                Math.min(extraImagePaths.size, mSelectedTemplateItem!!.photoItemList.size)
            for (i in 0 until size)
                mSelectedTemplateItem!!.photoItemList[i].imagePath = extraImagePaths[i]
        }

        binding.btnNext.setOnClickListener(this)

        binding.drawView.drawManager.addDrawInteractListener(this)

        binding.btnUndo.setOnClickListener { getDrawerManager()!!.undo() }

        binding.btnRedo.setOnClickListener { getDrawerManager()!!.redo() }

    }

    private fun saveTempBitmap(bitmap: Bitmap): Uri {
        val file = File(externalCacheDir, "collage_${System.currentTimeMillis()}.jpg")
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        fos.flush()
        fos.close()

        return FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )
    }

    private fun setUpTab() {
        val tools = mutableListOf(
            ToolItem(R.drawable.ic_layout, getString(R.string.layout)),
            ToolItem(R.drawable.ic_border, getString(R.string.border)),
            ToolItem(R.drawable.ic_sticker, getString(R.string.sticker)),
            ToolItem(R.drawable.ic_background, getString(R.string.background)),
            ToolItem(R.drawable.ic_text, getString(R.string.text)),
            ToolItem(R.drawable.ic_ratio, getString(R.string.ratio))
        )

        val adapter = ToolAdapter(tools) { item, pos ->
            when (pos) {
                0 -> { /* Layout */

                    binding.llFrame.visibility = View.VISIBLE
                    binding.llBorder.visibility = View.GONE
                    binding.llBg.visibility = View.GONE
                    binding.llSticker.visibility = View.GONE
                    binding.llDraw.visibility = View.GONE
                    binding.llRatio.visibility = View.GONE
                    binding.rcvTools.visibility = View.GONE

                    binding.icCheckLayout.setOnClickListener {
                        binding.llFrame.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }

                    binding.drawView.setDrawingEnabled(false)
                }

                1 -> { /* Border */

                    binding.llFrame.visibility = View.GONE
                    binding.llBorder.visibility = View.VISIBLE
                    binding.llBg.visibility = View.GONE
                    binding.llSticker.visibility = View.GONE
                    binding.llDraw.visibility = View.GONE
                    binding.llRatio.visibility = View.GONE
                    binding.rcvTools.visibility = View.GONE

                    binding.tvGrid.text = binding.seekbarSpace.progress.toString()
                    binding.tvCorner.text = binding.seekbarCorner.progress.toString()

                    binding.icCheckBorder.setOnClickListener {
                        binding.llBorder.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }

                    binding.drawView.setDrawingEnabled(false)
                }

                2 -> { /* Sticker */

                    binding.llFrame.visibility = View.GONE
                    binding.llBorder.visibility = View.GONE
                    binding.llBg.visibility = View.GONE
                    binding.llSticker.visibility = View.VISIBLE
                    binding.llDraw.visibility = View.GONE
                    binding.llRatio.visibility = View.GONE
                    binding.rcvTools.visibility = View.GONE


                    binding.icCheckSticker.setOnClickListener {
                        binding.llSticker.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                        binding.drawView.setDrawingEnabled(false)
                    }

                    binding.drawView.setDrawingEnabled(true)
                }

                3 -> { /* Background */

                    binding.llFrame.visibility = View.GONE
                    binding.llBorder.visibility = View.GONE
                    binding.llBg.visibility = View.VISIBLE
                    binding.llSticker.visibility = View.GONE
                    binding.llDraw.visibility = View.GONE
                    binding.llRatio.visibility = View.GONE
                    binding.rcvTools.visibility = View.GONE

                    binding.icCheckBackground.setOnClickListener {
                        binding.llBg.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }

                    binding.drawView.setDrawingEnabled(false)
                }

                4 -> { /* Text */

                    binding.llFrame.visibility = View.GONE
                    binding.llBorder.visibility = View.GONE
                    binding.llBg.visibility = View.GONE
                    binding.llSticker.visibility = View.GONE
                    binding.llRatio.visibility = View.GONE
                    binding.llDraw.visibility = View.VISIBLE
                    binding.rcvTools.visibility = View.GONE

                    binding.icBrush.setOnClickListener {
                        binding.icBrush.setBackgroundResource(R.drawable.bg_icon_draw)
                        binding.icErase.setBackgroundResource(0)
                        type = TYPE_GESTURE
                        updateDraw()
                    }

                    binding.icErase.setOnClickListener {
                        binding.icErase.setBackgroundResource(R.drawable.bg_icon_draw)
                        binding.icBrush.setBackgroundResource(0)
                        type = TYPE_ERASER
                        updateDraw()
                    }

                    binding.icCheckDraw.setOnClickListener {
                        binding.llDraw.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                        binding.drawView.setDrawingEnabled(false)
                    }

                    updateDraw()
                }

                5 -> { /* Ratio */

                    binding.llFrame.visibility = View.GONE
                    binding.llBorder.visibility = View.GONE
                    binding.llBg.visibility = View.GONE
                    binding.llSticker.visibility = View.GONE
                    binding.llDraw.visibility = View.GONE
                    binding.llRatio.visibility = View.VISIBLE
                    binding.rcvTools.visibility = View.GONE

                    binding.btnRotate.setOnClickListener {

                    }

                    binding.icCheckRatio.setOnClickListener {
                        binding.llRatio.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }

                    binding.drawView.setDrawingEnabled(false)
                }
            }
        }

        binding.rcvTools.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        binding.rcvTools.adapter = adapter
    }

    /* --- private --- */
    private fun updateDraw() {
        val drawPath: DrawPath?
        val size: Float
        gesturePaintStyle = PaintStyle.STROKE
        shapePaintStyle = PaintStyle.STROKE
        shapeBrushStyle = BrushStyle.HEART

        Log.e("xcncnah", "updateDraw: " + type.toString())

        when (type) {
            TYPE_GESTURE -> drawPath =
                DrawPath(
                    BrushStyle.GESTURE,
                    gesturePaintStyle,
                    color,
                    gestureSize
                )

            TYPE_SHAPE -> drawPath =
                DrawPath(shapeBrushStyle, shapePaintStyle, color, shapeSize)

            TYPE_ERASER -> drawPath =
                DrawPath(BrushStyle.GESTURE, PaintStyle.ERASE, color, eraserSize)

            else -> return
        }
        getDrawerManager()!!.setDrawPath(drawPath)
        binding.icBrush.isSelected = type == TYPE_GESTURE
        binding.icErase.isSelected = type == TYPE_ERASER

        binding.drawView.setDrawingEnabled(true)
    }

    private fun loadImageBeards() {
        val gson = Gson()
        val beard = object : TypeToken<MutableList<Beard?>?>() {
        }.getType()
        var beards: MutableList<Beard?>? = null
        try {
            beards = gson.fromJson<MutableList<Beard?>?>(
                InputStreamReader(
                    assets.open("beard.json")
                ), beard
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        binding.listSticker.setLayoutManager(
            LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL,
                false
            )
        )

        beardList = beards
        beardAdapter = BeardAdapter()
        beardAdapter?.setData(beardList)

        binding.listSticker.setAdapter(beardAdapter)
        binding.listSticker.smoothScrollToPosition(0)

        beardAdapter?.setClickListener { position, beard -> onSelectModel(beard.imageAsset) }
    }

    private fun loadFrameImages() {
        val mAllTemplateItemList = java.util.ArrayList<TemplateItem>()

        mAllTemplateItemList.addAll(FrameImageUtils.loadFrameImages(this))

        mTemplateItemList = java.util.ArrayList<TemplateItem>()
        if (mImageInTemplateCount > 0) {
            for (item in mAllTemplateItemList)
                if (item.photoItemList.size === mImageInTemplateCount) {
                    mTemplateItemList!!.add(item)
                }
        } else {
            mTemplateItemList!!.addAll(mAllTemplateItemList)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putFloat("mSpace", mSpace)
        outState.putFloat("mCornerBar", mCorner)
        if (mFramePhotoLayout != null) {
            mFramePhotoLayout!!.saveInstanceState(outState)
        }

    }

    fun buildLayout(item: TemplateItem) {

        mFramePhotoLayout = FramePhotoLayout(this, item.photoItemList)

        var viewWidth = binding.rlContainer.getWidth()
        var viewHeight = binding.rlContainer.getHeight()
        if (mLayoutRatio === RATIO_SQUARE) {
            if (viewWidth > viewHeight) {
                viewWidth = viewHeight
            } else {
                viewHeight = viewWidth
            }
        } else if (mLayoutRatio === RATIO_GOLDEN) {
            val goldenRatio = 1.61803398875
            if (viewWidth <= viewHeight) {
                if (viewWidth * goldenRatio >= viewHeight) {
                    viewWidth = (viewHeight / goldenRatio).toInt()
                } else {
                    viewHeight = (viewWidth * goldenRatio).toInt()
                }
            } else if (viewHeight <= viewWidth) {
                if (viewHeight * goldenRatio >= viewWidth) {
                    viewHeight = (viewWidth / goldenRatio).toInt()
                } else {
                    viewWidth = (viewHeight * goldenRatio).toInt()
                }
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
        //add sticker view
        binding.rlContainer.addView(binding.drawView, params)
        //reset space and corner seek bars

        binding.seekbarSpace.progress = (MAX_SPACE_PROGRESS * mSpace / MAX_SPACE).toInt()
        binding.seekbarCorner.progress = (MAX_CORNER_PROGRESS * mCorner / MAX_CORNER).toInt()
    }

    @Throws(OutOfMemoryError::class)
    fun createOutputImage(): Bitmap {
        try {
            val template = mFramePhotoLayout!!.createImage()

            val result = createBitmap(template.width, template.height)
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            if (mBackgroundImage != null) {
                canvas.drawBitmap(
                    mBackgroundImage!!, null,
                    Rect(0, 0, result.width, result.height), paint
                )
            } else {
                canvas.drawColor(mBackgroundColor)
            }

            canvas.drawBitmap(template, 0f, 0f, paint)

            val drawBitmap = getBitmapFromView(binding.drawView)
            canvas.drawBitmap(drawBitmap, 0f, 0f, paint)
            return result
        } catch (error: OutOfMemoryError) {
            throw error
        }
    }

    fun getBitmapFromView(view: View): Bitmap {
        val bitmap = createBitmap(view.width, view.height)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    fun onSelectModel(s: String?) {
        getDrawerManager()!!.addSticker(StickerData(s))
    }

    override fun interactStickerFocusChange(stickerData: StickerData?) {

    }

    override fun interactTouchDown() {

    }

    override fun interactTouchUp() {

    }

    override fun interactUndoRedoChange() {

    }

    override fun interactUpdateBackground(url: String?) {
//        Glide.with(this)
//            .asBitmap()
//            .load(url)
//            .into(binding.imgBackground)
    }
}
