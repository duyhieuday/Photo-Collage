package com.example.piceditor

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.piceditor.adapters.BackgroundAdapter
import com.example.piceditor.adapters.ToolAdapter
import com.example.piceditor.ads.InterAds
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityAfterRemoveBinding
import com.example.piceditor.draw.DrawInteractListener
import com.example.piceditor.draw.DrawerManager
import com.example.piceditor.draw.model.draw.DrawPath
import com.example.piceditor.draw.model.draw.style.BrushStyle
import com.example.piceditor.draw.model.draw.style.PaintStyle
import com.example.piceditor.draw.model.sticker.StickerData
import com.example.piceditor.draw.test.Beard
import com.example.piceditor.draw.test.BeardAdapter
import com.example.piceditor.model.ToolItem
import com.example.piceditor.utils.BarsUtils
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

class AfterRemoveActivity : BaseActivityNew<ActivityAfterRemoveBinding>(),
    BackgroundAdapter.OnBGClickListener,
    DrawInteractListener {

    companion object {
        const val EXTRA_SUBJECT_URL = "subject_url"
        const val TYPE_GESTURE = 0
        const val TYPE_ERASER  = 2
    }

    private var subjectUrl: String? = null

    // ── Draw state ────────────────────────────────────────
    private var drawerManager: DrawerManager? = null
    private var drawType: Int = TYPE_GESTURE
    private var drawColor: Int = Color.BLACK
    private var gestureSize: Float = 16f
    private var eraserSize: Float = 20f

    // ── Sticker state ─────────────────────────────────────
    private var beardAdapter: BeardAdapter? = null
    private var beardList: MutableList<Beard?>? = null

    // ─────────────────────────────────────────────────────────────────────────
    // BaseActivityNew overrides
    // ─────────────────────────────────────────────────────────────────────────

    override fun getLayoutRes(): Int = R.layout.activity_after_remove
    override fun getFrame(): Int = 0

    override fun getDataFromIntent() {
        subjectUrl = intent?.getStringExtra(EXTRA_SUBJECT_URL)
    }

    override fun doAfterOnCreate() {
        if (PreferenceUtil.getInstance(this)
                .getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")) {
            initBanner(binding.banner.adViewContainer)
        } else {
            initBanner(binding.adViewContainer)
            binding.banner.root.visibility = View.GONE
        }
    }

    override fun setListener() {
        binding.btnBack.setOnClickListener {
            InterAds.showAdsBreak(this@AfterRemoveActivity) { finish() }
        }

        binding.btnNext.setOnClickListener {
            // ✅ Check WRITE permission cho Android 9 trở xuống
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Cần quyền lưu trữ để lưu ảnh",
                        Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            InterAds.showAdsBreak(this@AfterRemoveActivity) {
                try {
                    MainActivity.isFromSaved = true
                    val bitmap = exportComposite()
                    val finalUri = saveToGallery(bitmap)
                    startActivity(Intent(this, ShowImageActivity::class.java).apply {
                        putExtra("image_uri", finalUri.toString())
                    })
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Lưu ảnh thất bại", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun initFragment(): BaseFragment<*>? = null

    override fun afterSetContentView() {
        super.afterSetContentView()
        BarsUtils.setHideNavigation(this)
        BarsUtils.setStatusBarColor(this, Color.parseColor("#01000000"))
        BarsUtils.setAppearanceLightStatusBars(this, true)
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
        InterAds.showAdsBreak(this) { super.onBackPressed() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.drawView.setDrawingEnabled(false)

        setupBackgroundList()
        loadImageBeards()
        setUpTab()

        binding.drawView.drawManager.addDrawInteractListener(this)
        syncUndoRedoUI()

        binding.btnUndo.setOnClickListener { getDrawerManager()?.undo() }
        binding.btnRedo.setOnClickListener { getDrawerManager()?.redo() }

        loadSubjectImage()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load subject
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadSubjectImage() {
        if (subjectUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Không có ảnh subject", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Glide.with(this).load(subjectUrl).into(binding.imgSubject)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tool tabs
    // ─────────────────────────────────────────────────────────────────────────

    private fun setUpTab() {
        val tools = mutableListOf(
            ToolItem(R.drawable.ic_background, getString(R.string.background)),
            ToolItem(R.drawable.ic_sticker,    getString(R.string.sticker)),
            ToolItem(R.drawable.ic_text,       getString(R.string.text)),
        )

        val adapter = ToolAdapter(tools) { _, pos ->
            binding.llBg.visibility      = View.GONE
            binding.llSticker.visibility = View.GONE
            binding.llDraw.visibility    = View.GONE
            binding.rcvTools.visibility  = View.GONE

            when (pos) {
                0 -> { // Background
                    binding.llBg.visibility = View.VISIBLE
                    binding.icCheckBackground.setOnClickListener {
                        binding.llBg.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                    }
                    binding.drawView.setDrawingEnabled(false)
                }

                1 -> { // Sticker
                    binding.llSticker.visibility = View.VISIBLE
                    binding.icCheckSticker.setOnClickListener {
                        binding.llSticker.visibility = View.GONE
                        binding.rcvTools.visibility = View.VISIBLE
                        binding.drawView.setDrawingEnabled(false)
                    }
                    binding.drawView.setDrawingEnabled(true)
                    syncUndoRedoUI()
                }

                2 -> { // Draw / Text
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
                        binding.drawView.setDrawingEnabled(false)
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

    // ─────────────────────────────────────────────────────────────────────────
    // Background tab
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupBackgroundList() {
        binding.listBg.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.listBg.adapter = BackgroundAdapter(this, this)
    }

    override fun onBGClick(drawable: Drawable) {
        val bitmap = (drawable as BitmapDrawable).bitmap
        binding.imgBackground.setImageBitmap(bitmap)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sticker tab
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadImageBeards() {
        val gson = Gson()
        val type = object : TypeToken<MutableList<Beard?>?>() {}.type
        val beards: MutableList<Beard?>? = try {
            gson.fromJson(InputStreamReader(assets.open("beard.json")), type)
        } catch (e: IOException) {
            null
        }

        binding.listSticker.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        beardList = beards
        beardAdapter = BeardAdapter()
        beardAdapter?.setData(beardList)
        binding.listSticker.adapter = beardAdapter
        beardAdapter?.setClickListener { _, beard -> onSelectModel(beard.imageAsset) }
    }

    private fun onSelectModel(s: String?) {
        getDrawerManager()?.addSticker(StickerData(s))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Draw tab
    // ─────────────────────────────────────────────────────────────────────────

    private fun getDrawerManager(): DrawerManager? {
        if (drawerManager == null) {
            drawerManager = binding.drawView.drawManager
        }
        return drawerManager
    }

    private fun updateDraw() {
        val drawPath: DrawPath = when (drawType) {
            TYPE_GESTURE -> DrawPath(BrushStyle.GESTURE, PaintStyle.STROKE, drawColor, gestureSize)
            TYPE_ERASER  -> DrawPath(BrushStyle.GESTURE, PaintStyle.ERASE,  drawColor, eraserSize)
            else -> return
        }
        getDrawerManager()?.setDrawPath(drawPath)
        binding.icBrush.isSelected = drawType == TYPE_GESTURE
        binding.icErase.isSelected = drawType == TYPE_ERASER
        binding.drawView.setDrawingEnabled(true)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Undo / Redo
    // ─────────────────────────────────────────────────────────────────────────

    private fun syncUndoRedoUI() {
        val manager = getDrawerManager() ?: return
        val canUndo = manager.isActiveUndo
        val canRedo = manager.isActiveRedo

        binding.btnUndo.isEnabled = canUndo
        binding.btnUndo.alpha = if (canUndo) 1f else 0.3f

        binding.btnRedo.isEnabled = canRedo
        binding.btnRedo.alpha = if (canRedo) 1f else 0.3f
    }

    override fun interactUndoRedoChange() { syncUndoRedoUI() }
    override fun interactStickerFocusChange(stickerData: StickerData?) {}
    override fun interactTouchDown() {}
    override fun interactTouchUp() {}
    override fun interactUpdateBackground(url: String?) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Export composite — composite cả 3 layer (BG + Subject + DrawView)
    // ─────────────────────────────────────────────────────────────────────────

    private fun exportComposite(): Bitmap {
        val container = binding.rlContainer
        val bitmap = Bitmap.createBitmap(
            container.width, container.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        container.draw(canvas)
        return bitmap
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save to gallery — compatible với mọi Android version (copy từ FilterCollageActivity)
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveToGallery(bitmap: Bitmap): Uri {
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToGalleryQ(bitmap, fileName)
        } else {
            saveToGalleryLegacy(bitmap, fileName)
        }
    }

    private fun saveToGalleryQ(bitmap: Bitmap, fileName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoCollage")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        contentResolver.openOutputStream(uri)?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        contentResolver.update(uri, values, null, null)
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveToGalleryLegacy(bitmap: Bitmap, fileName: String): Uri {
        val picturesDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val collageDir = File(picturesDir, "PhotoCollage")
        if (!collageDir.exists()) collageDir.mkdirs()

        val file = File(collageDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATA, file.absolutePath)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: Uri.fromFile(file)
    }
}