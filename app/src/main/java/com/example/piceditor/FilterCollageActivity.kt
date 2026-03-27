package com.example.piceditor

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piceditor.MainActivity.Companion.isFromSaved
import com.example.piceditor.adapters.FilterNameAdapter
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityFilterCollageBinding
import com.example.piceditor.model.FilterData
import com.example.piceditor.utils.AndroidUtils
import com.example.piceditor.utils.BarsUtils
import java.io.File
import java.io.FileOutputStream
import java.util.*


class FilterCollageActivity : BaseActivityNew<ActivityFilterCollageBinding>(), View.OnClickListener {

    private var mLastClickTime: Long = 0


    private fun checkClick() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()
    }

    override fun onClick(v: View?) {
    }


    private val screenShot: Bitmap
        get() {
            val findViewById = findViewById<View>(R.id.img_collage)
            findViewById.background = null
            findViewById.destroyDrawingCache()
            findViewById.isDrawingCacheEnabled = true
            val createBitmap = Bitmap.createBitmap(findViewById.drawingCache)
            findViewById.isDrawingCacheEnabled = false
            val createBitmap2 = createBitmap(createBitmap.width, createBitmap.height)
            findViewById.draw(Canvas(createBitmap2))
            return createBitmap2
        }

    private var savedImageUri: Uri? = null

    private fun saveBitmap(bitmap: Bitmap) {
        val now = Date()
        val myDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Camera");
        myDir.mkdirs()

        val fname: String = (now.time / 1000).toString() + ".png"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
//        file.createNewFile();
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            out.flush()
            out.close()
            savedImageUri = Uri.parse(file.path)



            MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), null) { path, uri ->
                Log.i("ExternalStorage", "Scanned $path:")
                Log.i("ExternalStorage", "-> uri=$uri")
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    companion object {
        var red: Float = 0F
        var green: Float = 0F
        var blue: Float = 0F
        var saturation: Float = 0F
    }

    lateinit var bmp: Bitmap

    override fun getLayoutRes(): Int {
        return R.layout.activity_filter_collage
    }

    override fun getFrame(): Int {
        return 0
    }

    override fun getDataFromIntent() {

    }

    override fun doAfterOnCreate() {

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
        val bitmapPath = this.cacheDir.absolutePath + "/tempBMP"
        bmp = BitmapFactory.decodeFile(bitmapPath)
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnNext.setOnClickListener {
            checkClick()
            isFromSaved = true
            try {
                saveBitmap(screenShot)
            } catch (th: Throwable) {
                th.printStackTrace()
            }
            val intent = Intent(this, ShowImageActivity::class.java)
            intent.putExtra("image_uri", savedImageUri!!.toString())
            startActivityForResult(intent, 2)
            finish()
        }

        binding.imgCollage.setImageBitmap(bmp)
        binding.listFilterstype.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        var filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_clr1)
        binding.listFilterstype.adapter = filter_typeAdapter

        binding.filterNames.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val filter_nameAdapter = FilterNameAdapter(this, resources.getStringArray(R.array.filters))

        filter_nameAdapter.setOnFilterNameClick(object : FilterNameAdapter.FilterNameClickListener {
            override fun onItemClick(view: View, position: Int) {

                when (position) {
                    0 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_clr1)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    1 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_clr2)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    2 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_duo)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    3 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_pink)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    4 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_fresh)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    5 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_euro)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    6 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_dark)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    7 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_ins)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    8 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_elegant)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    9 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_golden)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    10 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_tint)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    11 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_film)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    12 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_lomo)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    13 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_movie)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    14 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_retro)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    15 -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_bw)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                    else -> {
                        filter_typeAdapter = FilterDetailAdapter(AndroidUtils.filter_clr1)
                        binding.listFilterstype.adapter = filter_typeAdapter
                    }
                }
                filter_nameAdapter.notifyDataSetChanged()
                filter_typeAdapter.notifyDataSetChanged()
            }
        })

        binding.filterNames.adapter = filter_nameAdapter

    }

    inner class FilterDetailAdapter(filters: Array<FilterData>) :
        RecyclerView.Adapter<FilterDetailAdapter.FilterDetailHolder>() {
        var filterType = filters
        var selectedindex = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterDetailHolder {
            val view = LayoutInflater.from(this@FilterCollageActivity)
                .inflate(R.layout.item_filter, parent, false)
            return FilterDetailHolder(view)
        }

        override fun getItemCount(): Int {
            return filterType.size
        }

        override fun onBindViewHolder(
            holder: FilterDetailHolder,
            @SuppressLint("RecyclerView") position: Int
        ) {

            if (selectedindex == position) {
                holder.rl_filteritem.setBackgroundColor(resources.getColor(R.color.darkBrown))
            } else {
                holder.rl_filteritem.setBackgroundColor(resources.getColor(R.color.transparent))
            }

            holder.thumbnail_filter.setImageResource(R.drawable.thumb_filter)

            red = filterType[position].red
            green = filterType[position].green
            blue = filterType[position].blue
            saturation = filterType[position].saturation

            val bitmap = createBitmap(bmp.width, bmp.height)
            val canvas = Canvas(bitmap)

            val paint = Paint()
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(saturation)

            val colorScale = ColorMatrix()
            colorScale.setScale(
                red,
                green,
                blue, 1F
            )
            colorMatrix.postConcat(colorScale)

            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(bmp, 0F, 0F, paint)

            holder.thumbnail_filter.setImageBitmap(bitmap)

            holder.filterName.text = filterType[position].text

            holder.rl_filteritem.setOnClickListener {
                val imgCollage = findViewById<ImageView>(R.id.img_collage)

                selectedindex = position

                red = filterType[position].red
                green = filterType[position].green
                blue = filterType[position].blue
                saturation = filterType[position].saturation

                Async_Filter(
                    bmp,
                    imgCollage
                ).executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR,
                    red,
                    green,
                    blue
                )
                notifyDataSetChanged()
            }
        }

        inner class FilterDetailHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var thumbnail_filter: ImageView
            var filterName: TextView
            var rl_filteritem: RelativeLayout

            init {
                thumbnail_filter = itemView.findViewById(R.id.thumbnail_filter)
                filterName = itemView.findViewById(R.id.filterName)
                rl_filteritem = itemView.findViewById(R.id.rl_filteritem)
            }
        }
    }

    class Async_Filter() : AsyncTask<Float, Void, Bitmap>() {

        lateinit var originalBitmap: Bitmap
        @SuppressLint("StaticFieldLeak")
        lateinit var imgMain: ImageView

        constructor(originalBitmap: Bitmap, imgMain: ImageView) : this() {
            this.originalBitmap = originalBitmap
            this.imgMain = imgMain
        }

        override fun doInBackground(vararg params: Float?): Bitmap {
            val r = params[0]
            val g = params[1]
            val b = params[2]

            val bitmap = createBitmap(this.originalBitmap.width, this.originalBitmap.height)
            val canvas = Canvas(bitmap)

            val paint = Paint()
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(saturation)

            val colorScale = ColorMatrix()
            colorScale.setScale(r!!, g!!, b!!, 1F)
            colorMatrix.postConcat(colorScale)

            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(this.originalBitmap, 0F, 0F, paint)

            return bitmap
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)

            this.imgMain.setImageBitmap(result)
        }
    }

}
