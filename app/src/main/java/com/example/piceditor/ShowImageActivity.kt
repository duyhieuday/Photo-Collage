package com.example.piceditor

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.piceditor.adapters.TemplateAdapter
import com.example.piceditor.ads.InterAds
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityShowImageBinding
import com.example.piceditor.templates_editor.Template
import com.example.piceditor.templates_editor.TemplateEditorActivity
import com.example.piceditor.templates_editor.TemplatePickerActivity
import com.example.piceditor.utils.BarsUtils
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

class ShowImageActivity : BaseActivityNew<ActivityShowImageBinding>(), View.OnClickListener {

    private var bitmap: Bitmap? = null
    private var imageUri: Uri? = null

    // ✅ Cache file share — chỉ ghi 1 lần, reuse cho các lần share sau
    private var cachedShareFile: File? = null
    private var shareJob: Job? = null
    private var mLastClickTime: Long = 0
    private var templateAdapter: TemplateAdapter? = null
    private var templateList: MutableList<Template?>? = null

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

    private var bannerInitialized = false

    override fun onResume() {
        super.onResume()
        // ✅ Chỉ init banner 1 lần — tránh reload mỗi khi resume từ Facebook
        if (!bannerInitialized) {
            if (!PreferenceUtil.getInstance(this)
                    .getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")) {
                initBanner(binding.adViewContainer)
                binding.banner.root.visibility = View.GONE
            }
            bannerInitialized = true
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
        loadImage()
        setUpTemp()

        binding.btnBack.setOnClickListener {
            InterAds.showAdsBreak(this@ShowImageActivity) { finish() }
        }

        binding.icHome.setOnClickListener {
            InterAds.showAdsBreak(this@ShowImageActivity) {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
                finish()
            }
        }

        binding.btnMakeAnother.setOnClickListener {
            InterAds.showAdsBreak(this@ShowImageActivity) {
                startActivity(Intent(this, SelectImageActivity::class.java))
                finish()
            }
        }

        binding.icInstagram.setOnClickListener { shareToApp("com.instagram.android") }
        binding.icFaceBook.setOnClickListener  { shareToApp("com.facebook.katana") }
        binding.icTiktok.setOnClickListener    { shareToTikTok() }
        binding.icMore.setOnClickListener      { shareImage() }
    }

    private fun setUpTemp() {
        val gson = Gson()
        val type = object : TypeToken<MutableList<Template?>?>() {}.getType()
        val temps: MutableList<Template?>? = try {
            gson.fromJson(InputStreamReader(assets.open("temp.json")), type)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        binding.rcvTemplates.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        templateList    = temps
        templateAdapter = TemplateAdapter()
        templateAdapter?.setData(templateList)
        binding.rcvTemplates.adapter = templateAdapter
        binding.rcvTemplates.smoothScrollToPosition(0)

        // ✅ template.id là Int, TemplateRepository.findById() nhận String → toString()
        templateAdapter?.setClickListener { position, template ->
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return@setClickListener
            mLastClickTime = SystemClock.elapsedRealtime()
            InterAds.showAdsBreak(this@ShowImageActivity) {
                val intent = Intent(this, TemplateEditorActivity::class.java).apply {
                    putExtra(TemplateEditorActivity.EXTRA_TEMPLATE_ID, template?.id?.toString())
                }
                startActivity(intent)
            }
        }

        binding.tvSeeAllTemplate.setOnClickListener {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return@setOnClickListener
            mLastClickTime = SystemClock.elapsedRealtime()
            InterAds.showAdsBreak(this@ShowImageActivity) {
                startActivity(Intent(this, TemplatePickerActivity::class.java))
            }
        }
    }

    // ✅ Chỉ load ảnh 1 lần — nếu bitmap đã có thì không load lại
    private fun loadImage() {
        if (bitmap != null) {
            binding.imgShow.setImageBitmap(bitmap)
            return
        }
        imageUri?.let { uri ->
            Glide.with(this)
                .asBitmap()
                .load(uri)
                .override(1080, 1920)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        bitmap = resource
                        binding.imgShow.setImageBitmap(resource)
                        prepareShareFile(resource)
                    }
                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                })
        }
    }

    // ──────────────────────────────────────────────────────
    // Prepare share file (chạy background 1 lần duy nhất)
    // ──────────────────────────────────────────────────────

    private fun prepareShareFile(bmp: Bitmap) {
        shareJob?.cancel()
        shareJob = lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val f = File(externalCacheDir, "share_image.jpg")
                FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                cachedShareFile = f
            }
        }
    }

    // ──────────────────────────────────────────────────────
    // Get share URI — dùng file đã cache, không ghi lại
    // ──────────────────────────────────────────────────────

    private fun getShareUri(onReady: (Uri) -> Unit) {
        val file = cachedShareFile
        if (file != null && file.exists()) {
            // ✅ File đã sẵn sàng → trả về ngay, 0ms lag
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            onReady(uri)
            return
        }

        // File chưa sẵn sàng (bitmap vẫn đang load) → fallback ghi file rồi share
        val bmp = bitmap
        if (bmp == null) {
            Toast.makeText(this, "Image not ready, please wait", Toast.LENGTH_SHORT).show()
            return
        }
        shareJob?.cancel()
        shareJob = lifecycleScope.launch {
            val f = withContext(Dispatchers.IO) {
                val f = File(externalCacheDir, "share_image.jpg")
                FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                f
            }
            cachedShareFile = f
            val uri = FileProvider.getUriForFile(this@ShowImageActivity, "$packageName.provider", f)
            onReady(uri)
        }
    }

    // ──────────────────────────────────────────────────────
    // Share
    // ──────────────────────────────────────────────────────

    private fun shareToApp(packageName: String) {
        // ✅ Với Facebook: dùng imageUri gốc (MediaStore) thay vì FileProvider
        // Facebook kill/restart process nên cachedShareFile bị mất
        // MediaStore URI không cần FileProvider và không bị ảnh hưởng bởi restart
        val shareUri = getDirectShareUri()
        if (shareUri == null) {
            Toast.makeText(this, "Image not ready, please wait", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                setPackage(packageName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (e: Exception) {
            Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ Trả về URI phù hợp để share
    // Ưu tiên imageUri gốc (content:// từ MediaStore) — không bị mất khi Facebook restart
    // Fallback về FileProvider nếu không có URI gốc
    private fun getDirectShareUri(): Uri? {
        val original = imageUri
        if (original != null && original.scheme == "content") {
            return original  // MediaStore URI — Facebook đọc được trực tiếp
        }
        // Fallback: dùng cached file qua FileProvider
        val file = cachedShareFile
        return if (file != null && file.exists()) {
            FileProvider.getUriForFile(this, "$packageName.provider", file)
        } else null
    }

    private fun shareToTikTok() {
        val packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
        val shareUri = getDirectShareUri()
        if (shareUri == null) {
            Toast.makeText(this, "Image not ready, please wait", Toast.LENGTH_SHORT).show()
            return
        }
        val pkg = packages.firstOrNull {
            packageManager.getLaunchIntentForPackage(it) != null
        }
        if (pkg == null) {
            Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            setPackage(pkg)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    private fun shareImage() {
        val shareUri = getDirectShareUri()
        if (shareUri == null) {
            // File chưa sẵn sàng → fallback dùng getShareUri có coroutine
            getShareUri { uri ->
                startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Share Image"
                ))
            }
            return
        }
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Share Image"
        ))
    }

    override fun onClick(v: View) {}
    override fun onBackPressed() { finish() }
}