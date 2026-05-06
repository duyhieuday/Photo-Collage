package com.example.piceditor

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piceditor.MainActivity.Companion.isFromSaved
import com.example.piceditor.adapters.FilterNameAdapter
import com.example.piceditor.ads.InterAds
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityFilterCollageBinding
import com.example.piceditor.model.FilterData
import com.example.piceditor.utils.AndroidUtils
import com.example.piceditor.utils.BarsUtils
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil
import java.io.File
import java.io.FileOutputStream

class FilterCollageActivity : BaseActivityNew<ActivityFilterCollageBinding>(),
    View.OnClickListener {

    private var mLastClickTime: Long = 0

    private fun checkClick() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return
        mLastClickTime = SystemClock.elapsedRealtime()
    }

    override fun onClick(v: View?) {}

    private val screenShot: Bitmap
        get() {
            val view = findViewById<View>(R.id.img_collage)
            view.background = null
            view.destroyDrawingCache()
            view.isDrawingCacheEnabled = true
            val cache = Bitmap.createBitmap(view.drawingCache)
            view.isDrawingCacheEnabled = false
            val result = createBitmap(cache.width, cache.height)
            view.draw(Canvas(result))
            return result
        }

    companion object {
        var red: Float        = 1f
        var green: Float      = 1f
        var blue: Float       = 1f
        var saturation: Float = 1f

        // Sentinel: index 0 = original image
        const val INDEX_ORIGINAL = 0
    }

    lateinit var bmp: Bitmap

    override fun getLayoutRes(): Int = R.layout.activity_filter_collage
    override fun getFrame(): Int = 0
    override fun getDataFromIntent() {}

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

        val uri = Uri.parse(intent.getStringExtra("image_uri"))
        bmp = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
        binding.imgCollage.setImageBitmap(bmp)

        binding.ivBack.setOnClickListener {
            InterAds.showAdsBreak(this@FilterCollageActivity) { finish() }
        }
        binding.btnNext.setOnClickListener {
            checkClick()

            // Check WRITE permission for Android 9 and below before saving
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Storage permission required to save image",
                        Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            isFromSaved = true
            val finalUri = saveToGallery(screenShot)
            InterAds.showAdsBreak(this@FilterCollageActivity) {
                startActivity(Intent(this, ShowImageActivity::class.java).apply {
                    putExtra("image_uri", finalUri.toString())
                })
                finish()
            }
        }

        // Filter list
        binding.listFilterstype.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Create adapter with filter list + "Original" at the beginning
        var filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_clr1)
        binding.listFilterstype.adapter = filter_typeAdapter

        // By default select "Original" when entering the screen
        filter_typeAdapter.selectOriginal()

        // Filter name tabs
        binding.filterNames.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val filter_nameAdapter = FilterNameAdapter(this, resources.getStringArray(R.array.filters))

        filter_nameAdapter.setOnFilterNameClick(object : FilterNameAdapter.FilterNameClickListener {
            override fun onItemClick(view: View, position: Int) {
                val filters = when (position) {
                    0  -> AndroidUtils.filter_clr1
                    1  -> AndroidUtils.filter_clr2
                    2  -> AndroidUtils.filter_duo
                    3  -> AndroidUtils.filter_pink
                    4  -> AndroidUtils.filter_fresh
                    5  -> AndroidUtils.filter_euro
                    6  -> AndroidUtils.filter_dark
                    7  -> AndroidUtils.filter_ins
                    8  -> AndroidUtils.filter_elegant
                    9  -> AndroidUtils.filter_golden
                    10 -> AndroidUtils.filter_tint
                    11 -> AndroidUtils.filter_film
                    12 -> AndroidUtils.filter_lomo
                    13 -> AndroidUtils.filter_movie
                    14 -> AndroidUtils.filter_retro
                    15 -> AndroidUtils.filter_bw
                    else -> AndroidUtils.filter_clr1
                }
                filter_typeAdapter = FilterDetailAdapter(filters)
                binding.listFilterstype.adapter = filter_typeAdapter

                // When switching tabs, select "Original" so user knows where they are
                filter_typeAdapter.selectOriginal()

                filter_nameAdapter.notifyDataSetChanged()
            }
        })

        binding.filterNames.adapter = filter_nameAdapter
    }

    // ────────────────────────────────────────────────────────
    // Save to gallery — compatible with all Android versions
    // ────────────────────────────────────────────────────────

    private fun saveToGallery(bitmap: Bitmap): Uri {
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ : use MediaStore with RELATIVE_PATH (scoped storage)
            saveToGalleryQ(bitmap, fileName)
        } else {
            // Android 9- : write directly to file (requires WRITE_EXTERNAL_STORAGE)
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
        // Create Pictures/PhotoCollage directory
        val picturesDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val collageDir = File(picturesDir, "PhotoCollage")
        if (!collageDir.exists()) collageDir.mkdirs()

        // Write file to disk
        val file = File(collageDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        // Insert into MediaStore so the image appears in Gallery
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

    // Small thumbnail for fast filter list rendering (avoid OOM)
    private val thumbBmp: Bitmap by lazy {
        val maxSize = 120
        val scale = maxSize.toFloat() / maxOf(bmp.width, bmp.height)
        Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), false)
    }

    // ────────────────────────────────────────────────────────
    // Adapter
    // ────────────────────────────────────────────────────────

    inner class FilterDetailAdapter(filters: Array<FilterData>) :
        RecyclerView.Adapter<FilterDetailAdapter.VH>() {

        private val filterType = filters
        var selectedIndex = INDEX_ORIGINAL

        fun selectOriginal() {
            selectedIndex = INDEX_ORIGINAL
            binding.imgCollage.setImageBitmap(bmp)
            notifyDataSetChanged()
        }

        fun applyFilter(filterPos: Int) {
            if (filterPos < 0 || filterPos >= filterType.size) return
            selectedIndex = filterPos + 1

            val filter = filterType[filterPos]
            red        = filter.red
            green      = filter.green
            blue       = filter.blue
            saturation = filter.saturation

            AsyncFilter(bmp, binding.imgCollage)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, red, green, blue)

            notifyDataSetChanged()
        }

        override fun getItemCount() = filterType.size + 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(this@FilterCollageActivity)
                .inflate(R.layout.item_filter, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.rl_filteritem.setBackgroundResource(
                if (selectedIndex == position) R.drawable.bg_item_selected
                else android.R.color.transparent
            )

            if (position == INDEX_ORIGINAL) {
                // "Original": show thumbnail of the original image
                holder.thumbnail_filter.setImageBitmap(thumbBmp)
                holder.filterName.text = getString(R.string.original)
                holder.rl_filteritem.setOnClickListener { selectOriginal() }
            } else {
                // Actual filter
                val filterPos = position - 1
                val filter    = filterType[filterPos]

                // Render small thumbnail with filter (use thumbBmp for speed)
                val thumb  = createBitmap(thumbBmp.width, thumbBmp.height)
                val canvas = Canvas(thumb)
                val paint  = Paint()
                val cm = ColorMatrix().apply { setSaturation(filter.saturation) }
                val cs = ColorMatrix().apply { setScale(filter.red, filter.green, filter.blue, 1f) }
                cm.postConcat(cs)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(thumbBmp, 0f, 0f, paint)

                holder.thumbnail_filter.setImageBitmap(thumb)
                holder.filterName.text = filter.text
                holder.rl_filteritem.setOnClickListener { applyFilter(filterPos) }
            }
        }

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val thumbnail_filter: ImageView   = itemView.findViewById(R.id.thumbnail_filter)
            val filterName: TextView          = itemView.findViewById(R.id.filterName)
            val rl_filteritem: RelativeLayout = itemView.findViewById(R.id.rl_filteritem)
        }
    }

    // ────────────────────────────────────────────────────────
    // AsyncTask apply filter
    // ────────────────────────────────────────────────────────

    class AsyncFilter() : AsyncTask<Float, Void, Bitmap>() {

        lateinit var originalBitmap: Bitmap

        @SuppressLint("StaticFieldLeak")
        lateinit var imgMain: ImageView

        constructor(originalBitmap: Bitmap, imgMain: ImageView) : this() {
            this.originalBitmap = originalBitmap
            this.imgMain        = imgMain
        }

        override fun doInBackground(vararg params: Float?): Bitmap {
            val r = params[0]!!
            val g = params[1]!!
            val b = params[2]!!
            val bitmap = createBitmap(originalBitmap.width, originalBitmap.height)
            val canvas = Canvas(bitmap)
            val paint  = Paint()
            val cm = ColorMatrix().apply { setSaturation(saturation) }
            val cs = ColorMatrix().apply { setScale(r, g, b, 1f) }
            cm.postConcat(cs)
            paint.colorFilter = ColorMatrixColorFilter(cm)
            canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
            return bitmap
        }

        override fun onPostExecute(result: Bitmap?) {
            imgMain.setImageBitmap(result)
        }
    }
}