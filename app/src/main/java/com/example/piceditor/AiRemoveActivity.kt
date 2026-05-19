package com.example.piceditor

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private var bgRemoved = false
    private var resultUrl: String? = null

    // Handler cho fake progress
    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var fakeProgress = 0

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

            if (!bgRemoved) {
                // Chưa remove BG → bắt đầu xử lý
                startRemoveBackground()
            } else {
                // Đã remove BG → chuyển sang AfterRemoveActivity
                val intent = Intent(this, AfterRemoveActivity::class.java).apply {
                    putExtra(AfterRemoveActivity.EXTRA_SUBJECT_URL, resultUrl)
                }
                startActivity(intent)
                finish()
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

    override fun onDestroy() {
        workPresenter?.dispose()
        stopFakeProgress()
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
    // UI: Processing / Done
    // ─────────────────────────────────────────────────────────────────────────

    private fun showProcessingUI() {
        binding.llProcessing.visibility = View.VISIBLE
        binding.tvDone.visibility = View.GONE
        binding.lottieProcessing.playAnimation()
        startFakeProgress()
    }

    private fun showDoneUI() {
        binding.llProcessing.visibility = View.GONE
        binding.lottieProcessing.cancelAnimation()
        binding.tvDone.visibility = View.VISIBLE
        stopFakeProgress()
    }

    private fun hideAllOverlay() {
        binding.llProcessing.visibility = View.GONE
        binding.lottieProcessing.cancelAnimation()
        binding.tvDone.visibility = View.GONE
        stopFakeProgress()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fake progress 0 → 95% (chờ API trả về thì lên 100%)
    // ─────────────────────────────────────────────────────────────────────────

    private fun startFakeProgress() {
        fakeProgress = 0
        binding.tvProgress.text = "AI is working on it ...0%"

        progressRunnable = object : Runnable {
            override fun run() {
                if (fakeProgress < 95) {
                    // Tăng nhanh ở đầu, chậm dần về sau
                    val increment = when {
                        fakeProgress < 30 -> 3
                        fakeProgress < 60 -> 2
                        fakeProgress < 85 -> 1
                        else -> 1
                    }
                    fakeProgress += increment
                    binding.tvProgress.text = "AI is working on it ...${fakeProgress}%"
                    progressHandler.postDelayed(this, 150)
                }
            }
        }
        progressHandler.postDelayed(progressRunnable!!, 150)
    }

    private fun stopFakeProgress() {
        progressRunnable?.let { progressHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun finishProgress() {
        // Khi API trả về → nhảy lên 100%
        stopFakeProgress()
        fakeProgress = 100
        binding.tvProgress.text = "AI is working on it ...100%"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Remove background processing
    // ─────────────────────────────────────────────────────────────────────────

    private fun startRemoveBackground() {
        isProcessing = true
        binding.btnNext.isEnabled = false
        binding.btnNext.alpha = 0.5f
        showProcessingUI()

        Thread {
            try {
                val inputFile = prepareInputFile()
                runOnUiThread { callRemoveBgApi(inputFile) }
            } catch (e: IOException) {
                runOnUiThread {
                    onProcessingFailed("Không đọc được file ảnh")
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
            if (isDestroyed || isFinishing) return@removeBg

            if (result == null
                || result.value == null
                || result.value.url.isNullOrEmpty()) {
                onProcessingFailed("Remove background thất bại")
                return@removeBg
            }

            val url = result.value.url
            resultUrl = url

            // ✅ Hiện progress 100% trước khi load ảnh
            finishProgress()

            Glide.with(this)
                .load(url)
                .into(binding.imgBackground)

            // Đợi 1 chút cho user thấy "100%" rồi mới hiện badge Done
            progressHandler.postDelayed({
                if (!isDestroyed && !isFinishing) {
                    isProcessing = false
                    bgRemoved = true
                    binding.btnNext.isEnabled = true
                    binding.btnNext.alpha = 1f
                    showDoneUI()
                }
            }, 500)
        }
    }

    private fun onProcessingFailed(message: String) {
        isProcessing = false
        binding.btnNext.isEnabled = true
        binding.btnNext.alpha = 1f
        hideAllOverlay()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}