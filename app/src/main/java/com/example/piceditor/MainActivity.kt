package com.example.piceditor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityMainBinding
import com.example.piceditor.utils.BarsUtils
import com.ezt.pdfreader.photoeditor.data.PageInfo
import java.io.File

class MainActivity : BaseActivityNew<ActivityMainBinding>() {

    private var mLastClickTime: Long = 0

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

    }

    override fun setListener() {

    }

    override fun initFragment(): BaseFragment<*>? {
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BarsUtils.setHideNavigation(this)
        checkAndRequestPermission()
        setupClick()
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
        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
    }

    private fun setupClick() {

        binding.openGal.setOnClickListener {

            // ===== ADD =====
            openGallery()
        }

        binding.openCam.setOnClickListener {

            // ===== ADD =====
            openCamera()
        }

        binding.btnEditor.setOnClickListener {

            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return@setOnClickListener
            mLastClickTime = SystemClock.elapsedRealtime()

            startActivity(Intent(this, SelectImageActivity::class.java))
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

    // ================= END ADD =================
}