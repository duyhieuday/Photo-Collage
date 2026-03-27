package com.example.piceditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.example.piceditor.MainActivity
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityFilterCollageBinding
import com.example.piceditor.databinding.ActivityShowImageBinding
import com.example.piceditor.utils.BarsUtils
import java.io.File
import androidx.core.net.toUri

class ShowImageActivity : BaseActivityNew<ActivityShowImageBinding>(), View.OnClickListener {

    private var image_uri: String? = null
    private var saved_file: File? = null
    private var density: Float = 0.toFloat()
    private var D_height: Int = 0
    private var D_width: Int = 0
    private var display: DisplayMetrics? = null

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

        saved_file = File(image_uri!!)
        display = resources.displayMetrics
        density = resources.displayMetrics.density
        D_width = display!!.widthPixels
        D_height = (display!!.heightPixels.toFloat() - density * 150.0f).toInt()

        binding.imgShow.setImageURI(image_uri!!.toUri())

//        share.setOnClickListener {
//            Log.d("jejeshare","shareclicked")
//            val shareIntent = Intent(Intent.ACTION_SEND)
//            shareIntent.type = "image/*"
//            shareIntent.putExtra(Intent.EXTRA_STREAM, image_uri)
//            startActivity(Intent.createChooser(shareIntent, "Share image"))
//        }

        binding.icHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

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