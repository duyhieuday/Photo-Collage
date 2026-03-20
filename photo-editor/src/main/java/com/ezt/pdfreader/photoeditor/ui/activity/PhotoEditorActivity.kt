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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ezt.pdfreader.photoeditor.R
import com.ezt.pdfreader.photoeditor.data.EditMode
import com.ezt.pdfreader.photoeditor.data.PageInfo
import com.ezt.pdfreader.photoeditor.databinding.ActivityPhotoEditorBinding
import com.ezt.pdfreader.photoeditor.databinding.DialogPeSavingBinding
import com.ezt.pdfreader.photoeditor.ui.fragment.PhotoEditorFragment
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
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        }
    }

    private lateinit var binding: ActivityPhotoEditorBinding
    private val viewModel: PhotoEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // fix crash android 13TP1A
        intent.setExtrasClassLoader(PageInfo::class.java.classLoader)
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupPages(savedInstanceState)
        setupToolbar()
        setupBackPress()
        observeViewModel()
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
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @Suppress("DEPRECATION")
    private fun setupPages(savedInstanceState: Bundle?) {
        val pages: ArrayList<PageInfo>? = intent.getParcelableArrayListExtra(EXTRA_PAGES)

        if (pages.isNullOrEmpty()) {
            finish()
            return
        }

        viewModel.initPages(pages)

        // Add fragment
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, PhotoEditorFragment())
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            handleBackPress()
        }

        binding.btnSave.setOnClickListener {
            saveAndFinish()
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    private fun handleBackPress() {
        // TODO: Show confirmation dialog if there are unsaved changes
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe pages
                launch {
                    viewModel.pages.collect { pages ->
                        // Update title if needed
                        binding.toolbar.title = getString(R.string.pe_title_edit_images)
                    }
                }

                // Observe edit mode - hide toolbar when not in preview mode
                launch {
                    viewModel.editMode.collect { mode ->
                        val isPreview = mode == EditMode.PREVIEW
                        binding.appBar.isVisible = isPreview
                        // Update layout behavior to properly hide/show space
                        val params =
                            binding.fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams
                        params.behavior = if (isPreview) {
                            com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior()
                        } else {
                            null
                        }
                        binding.fragmentContainer.layoutParams = params
                    }
                }
            }
        }
    }

    private fun saveAndFinish() {
        binding.btnSave.isEnabled = false

        val total = viewModel.pages.value.size
        val showProgress = total > 1

        // Show saving dialog
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
                if (showProgress) {
                    dialogBinding.tvProgress.text = "$current/$total"
                }
            }

            dialog.dismiss()

            val resultIntent = Intent().apply {
                putParcelableArrayListExtra(RESULT_PAGES, ArrayList(resultUris))
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
