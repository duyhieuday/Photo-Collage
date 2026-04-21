package com.example.piceditor

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityShowImageBinding
import com.example.piceditor.utils.BarsUtils
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ShowImageActivity : BaseActivityNew<ActivityShowImageBinding>(), View.OnClickListener {

    private var bitmap: Bitmap? = null
    private var imageUri: Uri? = null

    private var mLastClickTime: Long = 0
    private fun checkClick(): Boolean {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return false
        mLastClickTime = SystemClock.elapsedRealtime()
        return true
    }

    override fun getLayoutRes(): Int = R.layout.activity_show_image
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageUri = intent.getStringExtra("image_uri")?.toUri()

        // ✅ Dùng Glide load ảnh — không bị null sau khi resume từ app khác
        imageUri?.let { uri ->
            Glide.with(this)
                .asBitmap()
                .load(uri)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        bitmap = resource
                        binding.imgShow.setImageBitmap(resource)
                    }
                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                        // Không clear bitmap để share vẫn hoạt động
                    }
                })
        }

        binding.btnBack.setOnClickListener {
            // ✅ Back về đúng màn trước thay vì luôn về MainActivity
            finish()
        }

        binding.icHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }

        binding.btnMakeAnother.setOnClickListener {
            startActivity(Intent(this, SelectImageActivity::class.java))
            finish()
        }

        binding.icInstagram.setOnClickListener { shareToApp("com.instagram.android") }
        binding.icFaceBook.setOnClickListener  { shareToApp("com.facebook.katana") }
        binding.icTiktok.setOnClickListener    { shareToTikTok() }
        binding.icMore.setOnClickListener      { shareImage() }
    }

    // ──────────────────────────────────────────────────────
    // Share
    // ──────────────────────────────────────────────────────

    // ✅ Ghi file trên IO thread để không block UI
    private fun getShareUri(onReady: (Uri) -> Unit) {
        val bmp = bitmap
        if (bmp == null) {
            Toast.makeText(this, "Ảnh chưa sẵn sàng", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                val f = File(externalCacheDir, "share_image.jpg")
                FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                f
            }
            val uri = FileProvider.getUriForFile(this@ShowImageActivity, "$packageName.provider", file)
            onReady(uri)
        }
    }

    private fun shareToApp(packageName: String) {
        getShareUri { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage(packageName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "App chưa được cài đặt", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareToTikTok() {
        val packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
        getShareUri { uri ->
            val pkg = packages.firstOrNull {
                packageManager.getLaunchIntentForPackage(it) != null
            }
            if (pkg == null) {
                Toast.makeText(this, "App chưa được cài đặt", Toast.LENGTH_SHORT).show()
                return@getShareUri
            }
            startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage(pkg)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        }
    }

    private fun shareImage() {
        getShareUri { uri ->
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Chia sẻ ảnh"
            ))
        }
    }

    override fun onClick(v: View) {}

    override fun onBackPressed() {
        finish()
    }
}