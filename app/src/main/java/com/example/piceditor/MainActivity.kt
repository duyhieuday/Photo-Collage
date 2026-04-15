package com.example.piceditor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piceditor.adapters.ImageAdapter
import com.example.piceditor.adapters.TemplateAdapter
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityMainBinding
import com.example.piceditor.templates_editor.Template
import com.example.piceditor.templates_editor.TemplatePickerActivity
import com.example.piceditor.utils.BarsUtils
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil
import com.ezt.pdfreader.photoeditor.data.PageInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : BaseActivityNew<ActivityMainBinding>() {

    private var mLastClickTime: Long = 0
    private var templateAdapter: TemplateAdapter? = null
    private var templateList: MutableList<Template?>? = null
    companion object {
        var isFromSaved: Boolean = true
    }

    // ================= ADD =================

    private var cameraUri: Uri? = null

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->

            if (success && cameraUri != null) {
                openEditor(listOf(cameraUri!!))
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->

            if (uris.isNotEmpty()) {
                openEditor(uris)
            }
        }

    private val editorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            val pages = PhotoEditorWithBannerActivity.getResultPages(result.data)

            pages?.forEach {
                Toast.makeText(this, "Edited: $it", Toast.LENGTH_SHORT).show()
            }
        }

    // ================= END ADD =================

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            val granted = permissions.entries.all { it.value }

            if (granted) {
                loadData()
            } else {
                Toast.makeText(
                    this,
                    "Permission denied. Please allow in Settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun getLayoutRes(): Int {
        return R.layout.activity_main
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
        setUpRecent()
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
        setupClick()
        setUpTemp()
        setUpRecent()

//        binding.tvTemplates.setOnClickListener {
//            checkAndRequestPermission()
//            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return@setOnClickListener
//            mLastClickTime = SystemClock.elapsedRealtime()
//
//            startActivity(Intent(this, TemplatePickerActivity::class.java))
//        }
    }

    private fun setUpRecent(){
        val images = MyDraftActivity.getSavedImages(this)

        if (images.isEmpty()) {
            binding.llRecent.visibility = View.VISIBLE
            binding.rcvRecent.visibility = View.GONE
        } else {
            binding.rcvRecent.visibility = View.VISIBLE
            binding.llRecent.visibility = View.GONE
        }

        val adapter = ImageAdapter(images)

        binding.rcvRecent.setLayoutManager(
            LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        binding.rcvRecent.setAdapter(adapter)

        binding.tvSeeAll.setOnClickListener {
            checkAndRequestPermission()
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return@setOnClickListener
            mLastClickTime = SystemClock.elapsedRealtime()

            startActivity(Intent(this, MyDraftActivity::class.java))
        }

    }

    private fun setUpTemp(){
        val gson = Gson()
        val temp = object : TypeToken<MutableList<Template?>?>() {
        }.getType()
        var temps: MutableList<Template?>? = null
        try {
            temps = gson.fromJson<MutableList<Template?>?>(
                InputStreamReader(
                    assets.open("temp.json")
                ), temp
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        Log.e("xcncnajh1", "setUpTemp:  " + temps.size )
        temps?.forEach {
            Log.e("TEMP_JSON", "image = ${it?.image}")
        }

        binding.rcvTemplates.setLayoutManager(
            LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL,
                false
            )
        )

        templateList = temps
        templateAdapter = TemplateAdapter()
        templateAdapter?.setData(templateList)

        binding.rcvTemplates.setAdapter(templateAdapter)
        binding.rcvTemplates.smoothScrollToPosition(0)

        templateAdapter?.setClickListener { position, temp ->

        }
    }

    private fun checkAndRequestPermission() {

        val permissionList = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionList.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        permissionList.add(Manifest.permission.CAMERA)

        val notGranted = permissionList.filter { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            loadData()
        } else {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun loadData() {
//        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
    }

    private fun setupClick() {
        checkAndRequestPermission()

        binding.btnMenu.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        binding.llEdit.setOnClickListener {

            // ===== ADD =====
            openGallery()
        }

//        binding.openCam.setOnClickListener {
//
//            // ===== ADD =====
//            openCamera()
//        }

        binding.llCollage.setOnClickListener {
            checkAndRequestPermission()
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return@setOnClickListener
            mLastClickTime = SystemClock.elapsedRealtime()

            startActivity(Intent(this, SelectImageActivity::class.java))
        }

        binding.llCreate.setOnClickListener {
            checkAndRequestPermission()
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return@setOnClickListener
            mLastClickTime = SystemClock.elapsedRealtime()

            startActivity(Intent(this, SelectImageActivity::class.java))
        }

        binding.llDraft.setOnClickListener {
            checkAndRequestPermission()
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return@setOnClickListener
            mLastClickTime = SystemClock.elapsedRealtime()

            startActivity(Intent(this, MyDraftActivity::class.java))
        }

    }

    // ================= ADD =================

    private fun openCamera() {

        val file = File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")

        cameraUri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            file
        )

        cameraLauncher.launch(cameraUri)
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun openEditor(uris: List<Uri>) {

        val pages = uris.map { PageInfo(it) }

        val intent = PhotoEditorWithBannerActivity.createIntent(this, pages)

        editorLauncher.launch(intent)
    }
}