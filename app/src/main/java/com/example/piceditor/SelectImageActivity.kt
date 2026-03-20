package com.example.piceditor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piceditor.adapters.SelectedPhotoAdapter
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivitySelectImageBinding
import com.example.piceditor.uiFragments.GalleryAlbumFragment
import com.example.piceditor.uiFragments.GalleryAlbumImageFragment
import com.example.piceditor.utils.BarsUtils
import java.io.File

class SelectImageActivity : BaseActivityNew<ActivitySelectImageBinding>(), GalleryAlbumImageFragment.OnSelectImageListener,
    SelectedPhotoAdapter.OnDeleteButtonClickListener {


    override fun onDeleteButtonClick(str: String) {

        mSelectedImages.remove(str)
        mSelectedPhotoAdapter.notifyDataSetChanged()
        val textView = binding.textImgcount
        val str2 = "Select upto 10 photo(s)"
        val sb = StringBuilder()
        sb.append("(")
        sb.append(this.mSelectedImages.size)
        sb.append(")")
        textView.text = str2 + sb.toString()
    }

    private val mSelectedImages = ArrayList<String>()
    private var maxIamgeCount = 10
    private lateinit var mSelectedPhotoAdapter: SelectedPhotoAdapter
    private var mLastClickTime: Long = 0
    fun checkClick() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()
    }


    override fun onSelectImage(str: String) {
        if (this.mSelectedImages.size == this.maxIamgeCount) {
            Toast.makeText(
                this,
                String.format("You only need %d photo(s)", maxIamgeCount),
                Toast.LENGTH_SHORT
            )
                .show()
        } else {
            var uri = Uri.fromFile(File(str))

            this.mSelectedImages.add(str)
            this.mSelectedPhotoAdapter.notifyDataSetChanged()
            val textView = binding.textImgcount
            val str2 = "Select upto 10 photo(s)"
            val sb = StringBuilder()
            sb.append("(")
            sb.append(this.mSelectedImages.size)
            sb.append(")")
            textView.text = str2 + sb.toString()
        }
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_select_image
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

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BarsUtils.setHideNavigation(this)

        val back = findViewById<ImageView>(R.id.back)
        back.setOnClickListener{
            onBackPressed()
        }

        mSelectedPhotoAdapter = SelectedPhotoAdapter(mSelectedImages, this)

        binding.listImages.hasFixedSize()
        binding.listImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.listImages.adapter = mSelectedPhotoAdapter

        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_container, GalleryAlbumFragment(this)).commit()

        binding.btnNext.setOnClickListener {
            checkClick()
            createCollage()
        }
    }

    fun createCollage() {
        if (mSelectedImages.isEmpty()) {
            Toast.makeText(this, "Please select photo(s)", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(this, CollageActivity::class.java)
            intent.putExtra("imageCount", mSelectedImages.size)
            intent.putExtra("selectedImages", mSelectedImages)
            intent.putExtra("imagesinTemplate", mSelectedImages.size)

            startActivityForResult(intent, 111)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        if (requestCode == 111) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
