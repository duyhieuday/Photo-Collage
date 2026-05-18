package com.example.piceditor

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.piceditor.ads.InterAds
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityAiRemoveBinding
import com.example.piceditor.sever.ai_remove_bg.presenter.WorkPresenter
import com.example.piceditor.utils.BarsUtils
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class AiRemoveActivity : BaseActivityNew<ActivityAiRemoveBinding>() {

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
    }

    private var imagePath: String? = null
    private var workPresenter: WorkPresenter? = null
    private var isProcessing = false

    // ─────────────────────────────────────────────────────────────────────────
    // BaseActivityNew overrides
    // ─────────────────────────────────────────────────────────────────────────

    override fun getLayoutRes(): Int = R.layout.activity_ai_remove
    override fun getFrame(): Int = 0

    override fun getDataFromIntent() {
        imagePath = intent?.getStringExtra(EXTRA_IMAGE_PATH)
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
            if (isProcessing) {
                Toast.makeText(this, "Đang xử lý, vui lòng đợi...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            InterAds.showAdsBreak(this@AiRemoveActivity) { finish() }
        }

        binding.btnNext.setOnClickListener {
            if (imagePath.isNullOrEmpty()) {
                Toast.makeText(this, "Chưa có ảnh để xử lý", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startRemoveBackground()
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

    override fun onDestroy() {
        workPresenter?.dispose()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (isProcessing) {
            Toast.makeText(this, "Đang xử lý, vui lòng đợi...", Toast.LENGTH_SHORT).show()
            return
        }
        InterAds.showAdsBreak(this) { super.onBackPressed() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workPresenter = WorkPresenter()
        loadImageToView()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Image loading
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadImageToView() {
        val path = imagePath
        if (path.isNullOrEmpty()) {
            Toast.makeText(this, "Không tìm thấy ảnh", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val source: Any = if (path.startsWith("content://") || path.startsWith("file://")) {
            Uri.parse(path)
        } else {
            File(path)
        }

        Glide.with(this).load(source).into(binding.imgBackground)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Remove background processing
    // ─────────────────────────────────────────────────────────────────────────

    private fun setLoading(loading: Boolean) {
        if (isDestroyed || isFinishing) return
        isProcessing = loading
        binding.progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnNext.isEnabled = !loading
        binding.btnNext.alpha = if (loading) 0.5f else 1f
    }

    private fun startRemoveBackground() {
        setLoading(true)

        Thread {
            try {
                val inputFile = prepareInputFile()
                runOnUiThread { callRemoveBgApi(inputFile) }
            } catch (e: IOException) {
                runOnUiThread {
                    setLoading(false)
                    Toast.makeText(this, "Không đọc được file ảnh", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    @Throws(IOException::class)
    private fun prepareInputFile(): File {
        val path = imagePath ?: throw IOException("imagePath is null")

        return when {
            path.startsWith("content://") -> {
                val uri = Uri.parse(path)
                val resolver: ContentResolver = contentResolver
                val outFile = File(
                    cacheDir,
                    "ai_remove_input_${System.currentTimeMillis()}.jpg"
                )
                resolver.openInputStream(uri)?.use { input: InputStream ->
                    FileOutputStream(outFile).use { output ->
                        val buf = ByteArray(8192)
                        var n: Int
                        while (input.read(buf).also { n = it } > 0) {
                            output.write(buf, 0, n)
                        }
                    }
                } ?: throw IOException("Cannot open input stream for $uri")
                outFile
            }
            path.startsWith("file://") -> {
                val filePath = Uri.parse(path).path ?: throw IOException("Invalid file uri: $path")
                File(filePath)
            }
            else -> File(path)
        }
    }

    private fun callRemoveBgApi(inputFile: File) {
        workPresenter?.removeBg(inputFile) { result ->
            setLoading(false)

            if (result == null
                || result.value == null
                || result.value.url.isNullOrEmpty()) {
                Toast.makeText(this, "Remove background thất bại", Toast.LENGTH_SHORT).show()
                return@removeBg
            }

            val resultUrl = result.value.url

            // ✅ Remove BG thành công → chuyển sang AfterRemoveActivity
            val intent = Intent(this, AfterRemoveActivity::class.java).apply {
                putExtra(AfterRemoveActivity.EXTRA_SUBJECT_URL, resultUrl)
            }
            startActivity(intent)
            finish()
        }
    }
}