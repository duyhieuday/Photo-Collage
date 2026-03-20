package com.ezt.pdfreader.photoeditor.ui.activity

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.ezt.pdfreader.photoeditor.R
import com.ezt.pdfreader.photoeditor.data.PageInfo
import com.ezt.pdfreader.photoeditor.databinding.ActivityPhotoEditorBinding
import com.ezt.pdfreader.photoeditor.databinding.DialogPeDiscardBinding
import com.ezt.pdfreader.photoeditor.databinding.DialogPeSavingBinding
import com.ezt.pdfreader.photoeditor.ui.fragment.PhotoCropperFragment
import com.ezt.pdfreader.photoeditor.ui.fragment.PhotoFilterFragment
import com.ezt.pdfreader.photoeditor.viewmodel.PhotoEditorViewModel
import kotlinx.coroutines.launch

abstract class PhotoEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PAGES = "extra_pages"
        const val RESULT_PAGES = "result_pages"

        @Suppress("DEPRECATION")
        fun getResultPages(data: Intent?): List<Uri>? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableArrayListExtra(RESULT_PAGES, Uri::class.java)
            } else {
                data?.getParcelableArrayListExtra(RESULT_PAGES)
            }
        }

        fun clearCache(context: Context) {
            val cacheDir = java.io.File(context.cacheDir, "edited_pages")
            if (cacheDir.exists()) cacheDir.deleteRecursively()
        }
    }

    private lateinit var binding: ActivityPhotoEditorBinding
    val viewModel: PhotoEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        intent.setExtrasClassLoader(PageInfo::class.java.classLoader)
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindowInsets()
        setupPages(savedInstanceState)
        setupBackPress()
        initBannerAds()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearGlideCache()
    }

    abstract fun initBannerAds()
    abstract fun isPremium(): Boolean
    abstract fun openPaywall()

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    @Suppress("DEPRECATION")
    private fun setupPages(savedInstanceState: Bundle?) {
        val pages: ArrayList<PageInfo>? = intent.getParcelableArrayListExtra(EXTRA_PAGES)
        if (pages.isNullOrEmpty()) { finish(); return }
        viewModel.initPages(pages)
        if (savedInstanceState == null) replaceCrop()
    }

    fun replaceCrop() {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, PhotoCropperFragment())
        }
    }

    fun replaceFilter() {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, PhotoFilterFragment())
        }
    }

    fun openCropForCurrentPage() {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, PhotoCropperFragment.newSinglePageInstance())
            addToBackStack(null)
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If FilterFragment is on the back stack, pop it (goes back to CropperFragment)
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    showDiscardDialog()
                }
            }
        })
    }

    private fun showDiscardDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val b = DialogPeDiscardBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)
        b.btnCancel.setOnClickListener { dialog.dismiss() }
        b.btnDiscard.setOnClickListener {
            dialog.dismiss()
            setResult(RESULT_CANCELED)
            finish()
        }
        dialog.show()
    }

    /**
     * Called by PhotoFilterFragment when user presses Save.
     */
    fun saveAndFinish() {
        val total = viewModel.pages.value.size
        val showProgress = total > 1

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.setCancelable(false)
        val dialogBinding = DialogPeSavingBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        if (showProgress) {
            dialogBinding.tvProgress.isVisible = true
            dialogBinding.tvProgress.text = "0/$total"
        }
        dialog.show()

        lifecycleScope.launch {
            val resultUris = viewModel.saveEditedPages { current, _ ->
                if (showProgress) dialogBinding.tvProgress.text = "$current/$total"
            }
            dialog.dismiss()
            val resultIntent = Intent().apply {
                putParcelableArrayListExtra(RESULT_PAGES, ArrayList(resultUris))
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
