package com.example.piceditor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piceditor.adapters.GalleryImageAdapter
import com.example.piceditor.adapters.SelectedPhotoAdapter
import com.example.piceditor.ads.InterAds
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivitySelectImageBinding
import com.example.piceditor.uiFragments.GalleryFragment
import com.example.piceditor.utils.BarsUtils
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil

class SelectImageActivity : BaseActivityNew<ActivitySelectImageBinding>(),
    GalleryFragment.OnSelectImageListener,
    SelectedPhotoAdapter.OnDeleteButtonClickListener {

    private val mSelectedImages = ArrayList<String>()
    private var maxIamgeCount = 10
    private lateinit var mSelectedPhotoAdapter: SelectedPhotoAdapter
    private var mLastClickTime: Long = 0

    // ✅ Reference đến GalleryImageAdapter để sync badge khi xóa
    private var galleryAdapter: GalleryImageAdapter? = null

    fun setGalleryAdapter(adapter: GalleryImageAdapter) {
        galleryAdapter = adapter
    }

    fun checkClick() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return
        mLastClickTime = SystemClock.elapsedRealtime()
    }

    // ── Callbacks ─────────────────────────────────────────

    override fun onSelectImage(str: String) {
        if (mSelectedImages.size >= maxIamgeCount) {
            Toast.makeText(
                this,
                String.format("You can only select up to %d photo(s)", maxIamgeCount),
                Toast.LENGTH_SHORT
            ).show()
            return  // ← không add, không tăng badge
        }

        // ✅ Được phép chọn → add vào list và tăng badge
        mSelectedImages.add(str)
        galleryAdapter?.increaseCount(str)

        mSelectedPhotoAdapter.notifyDataSetChanged()
        binding.textImageCount.text = "Select upto 10 photo(s) (${mSelectedImages.size})"
        updateButtonState()
    }

    override fun onDeleteButtonClick(str: String) {
        // Chỉ xóa 1 lần xuất hiện (remove first occurrence)
        mSelectedImages.remove(str)

        // ✅ Giảm count badge trên gallery
        galleryAdapter?.decreaseCount(str)

        mSelectedPhotoAdapter.notifyDataSetChanged()
        binding.textImageCount.text = "Select upto 10 photo(s) (${mSelectedImages.size})"
        updateButtonState()
    }

    // ── UI helpers ────────────────────────────────────────

    private fun updateButtonState() {
        if (mSelectedImages.isEmpty()) {
            binding.btnNext.setBackgroundResource(R.drawable.bg_btn_next)
            binding.btnNext.setTextColor("#1D2939".toColorInt())
        } else {
            binding.btnNext.setBackgroundResource(R.drawable.bg_btn_next1)
            binding.btnNext.setTextColor("#FCFCFD".toColorInt())
        }
    }

    // ── BaseActivityNew overrides ─────────────────────────

    override fun getLayoutRes() = R.layout.activity_select_image
    override fun getFrame() = 0
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

    // ── Lifecycle ─────────────────────────────────────────

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BarsUtils.setHideNavigation(this)

        binding.ivBack.setOnClickListener {
            InterAds.showAdsBreak(this@SelectImageActivity) { finish() }
        }

        mSelectedPhotoAdapter = SelectedPhotoAdapter(mSelectedImages, this)
        binding.listImages.hasFixedSize()
        binding.listImages.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.listImages.adapter = mSelectedPhotoAdapter

        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_container, GalleryFragment())
            .commit()

        binding.btnNext.setOnClickListener {
            checkClick()
            createCollage()
        }
    }

    // ── Navigation ────────────────────────────────────────

    fun createCollage() {
        if (mSelectedImages.isEmpty()) {
            Toast.makeText(this, "Please select photo(s)", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            InterAds.showAdsBreak(this@SelectImageActivity) {
                val intent = Intent(this, CollageActivity::class.java)
                intent.putExtra("imageCount", mSelectedImages.size)
                intent.putExtra("selectedImages", mSelectedImages)
                intent.putExtra("imagesinTemplate", mSelectedImages.size)
                startActivityForResult(intent, 111)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        if (requestCode == 111) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}