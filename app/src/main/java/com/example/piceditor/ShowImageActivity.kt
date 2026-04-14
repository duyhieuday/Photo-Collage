package com.example.piceditor

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Base64
import android.util.DisplayMetrics
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityShowImageBinding
import com.example.piceditor.utils.BarsUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.net.toUri
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil

class ShowImageActivity : BaseActivityNew<ActivityShowImageBinding>(), View.OnClickListener {

    private var image_uri: String? = null
    private var saved_file: File? = null
    private var density: Float = 0.toFloat()
    private var D_height: Int = 0
    private var D_width: Int = 0
    private var display: DisplayMetrics? = null
    lateinit var bitmap: Bitmap


    private var mLastClickTime: Long = 0
    private fun checkClick() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_show_image
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        image_uri = intent.getStringExtra("image_uri")

//        saved_file = File(image_uri!!)
        display = resources.displayMetrics
        density = resources.displayMetrics.density
        D_width = display!!.widthPixels
        D_height = (display!!.heightPixels.toFloat() - density * 150.0f).toInt()

        val uri = intent.getStringExtra("image_uri")?.toUri()
        binding.imgShow.setImageURI(uri)

        binding.imgShow.post {
            bitmap = (binding.imgShow.drawable as BitmapDrawable).bitmap
        }

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.icHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnMakeAnother.setOnClickListener {
            val intent = Intent(this, SelectImageActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.icInstagram.setOnClickListener {
            shareToApp("com.instagram.android")
        }

        binding.icFaceBook.setOnClickListener {
            shareToApp("com.facebook.katana")
        }

        binding.icTiktok.setOnClickListener {
            shareToTikTok()
        }

        binding.icMore.setOnClickListener {
            shareImage()
        }

    }

    private fun shareToTikTok() {
        val packages = listOf(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill"
        )

        if (!::bitmap.isInitialized) {
            Toast.makeText(this, "Image not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val file = bitmapToFile(bitmap)
        val uri = getContentUri(file)

        for (pkg in packages) {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "image/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.setPackage(pkg)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                startActivity(shareIntent)
                return
            }
        }

        Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show()
    }

    private fun shareToApp(packageName: String) {
        if (!::bitmap.isInitialized) {
            Toast.makeText(this, "Image not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val file = bitmapToFile(bitmap)
        val uri = getContentUri(file)

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.setPackage(packageName)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareImage() {
        if (!::bitmap.isInitialized) {
            Toast.makeText(this, "Image not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val file = bitmapToFile(bitmap)
        val uri = getContentUri(file)

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(Intent.createChooser(intent, "Share Image"))
    }

    fun bitmapToFile(bitmap: Bitmap): File {
        val file = File(externalCacheDir, "share_image.jpg")

        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        fos.flush()
        fos.close()

        return file
    }

    private fun getContentUri(file: File): Uri {
        return FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            file
        )
    }

    override fun onClick(v: View) {
        when (v.id) {

        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}