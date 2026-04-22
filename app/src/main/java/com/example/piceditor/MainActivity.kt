package com.example.piceditor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
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

    private var cameraUri: Uri? = null

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraUri != null) openEditor(listOf(cameraUri!!))
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) openEditor(uris)
        }

    private val editorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val pages = PhotoEditorWithBannerActivity.getResultPages(result.data)
            pages?.forEach { Toast.makeText(this, "Edited: $it", Toast.LENGTH_SHORT).show() }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                loadData()
            } else {
                val deniedList = permissions.entries.filter { !it.value }.map { it.key }

                // shouldShowRequestPermissionRationale = true  → từ chối, chưa tick "Không hỏi lại"
                // shouldShowRequestPermissionRationale = false → có thể là:
                //   1. Lần đầu tiên (chưa hỏi bao giờ) — đã được track bằng hasRequestedBefore
                //   2. Đã tick "Không hỏi lại"
                val canAskAgain = deniedList.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                }

                if (canAskAgain) {
                    // Từ chối nhưng chưa tick "Không hỏi lại" → hỏi lại với dialog giải thích
                    showPermissionRationaleDialog(deniedList)
                } else if (hasRequestedBefore()) {
                    // Đã hỏi trước đó + shouldShow = false → đã tick "Không hỏi lại"
                    showGoToSettingsDialog()
                } else {
                    // Không nên xảy ra, nhưng fallback
                    showPermissionRationaleDialog(deniedList)
                }
                // Đánh dấu đã request ít nhất 1 lần
                markRequestedBefore()
            }
        }

    // ── Track lần đầu request ──────────────────────────────
    private val PREF_NAME         = "permission_pref"
    private val KEY_HAS_REQUESTED = "has_requested_permission"

    private fun hasRequestedBefore(): Boolean {
        return getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            .getBoolean(KEY_HAS_REQUESTED, false)
    }

    private fun markRequestedBefore() {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAS_REQUESTED, true)
            .apply()
    }

    // ──────────────────────────────────────────────────────
    // BaseActivityNew overrides
    // ──────────────────────────────────────────────────────

    override fun getLayoutRes(): Int = R.layout.activity_main
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

    private fun checkAndRequestPermission() {
        val notGranted = getRequiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        // ✅ Luôn kiểm tra đã có quyền chưa TRƯỚC TIÊN — kể cả sau khi cấp thủ công
        if (notGranted.isEmpty()) {
            loadData()
            return
        }

        when {
            // Đã từ chối, chưa tick "Không hỏi lại" → giải thích
            notGranted.any {
                ActivityCompat.shouldShowRequestPermissionRationale(this, it)
            } -> showPermissionRationaleDialog(notGranted)

            // Đã tick "Không hỏi lại" (đã request trước + shouldShow=false)
            hasRequestedBefore() -> showGoToSettingsDialog()

            // Lần đầu hỏi
            else -> {
                markRequestedBefore()
                permissionLauncher.launch(notGranted.toTypedArray())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!PreferenceUtil.getInstance(this)
                .getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")) {
            initBanner(binding.adViewContainer)
            binding.banner.root.visibility = View.GONE
        }
        setUpRecent()

        // ✅ Khi resume từ Settings sau khi cấp quyền thủ công
        // notGranted.isEmpty() → loadData(), không hiện bất kỳ dialog nào
        val notGranted = getRequiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isEmpty()) loadData()
    }

    override fun afterSetContentView() {
        super.afterSetContentView()
        BarsUtils.setHideNavigation(this)
        BarsUtils.setStatusBarColor(this, "#01000000".toColorInt())
        BarsUtils.setAppearanceLightStatusBars(this, true)
    }

    // ──────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupClick()
        setUpTemp()
        setUpRecent()

//        binding.tvTemplates.setOnClickListener {
//            checkAndRequestPermission()
//            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return@setOnClickListener
//            mLastClickTime = SystemClock.elapsedRealtime()
//            startActivity(Intent(this, TemplatePickerActivity::class.java))
//        }
    }

    // ──────────────────────────────────────────────────────
    // Permission
    // ──────────────────────────────────────────────────────

    private fun getRequiredPermissions(): List<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
//        list.add(Manifest.permission.CAMERA)
        return list
    }

    // ✅ Custom dialog giải thích khi user từ chối lần trước
    private fun showPermissionRationaleDialog(permissions: List<String>) {
        val dialog = android.app.Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_permission_rationale, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        view.findViewById<android.widget.Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<android.widget.Button>(R.id.btnGrant).setOnClickListener {
            dialog.dismiss()
            permissionLauncher.launch(permissions.toTypedArray())
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    // ✅ Custom dialog hướng dẫn vào Settings khi user tick "Không hỏi lại"
    private fun showGoToSettingsDialog() {
        val dialog = android.app.Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_permission_setting, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        view.findViewById<android.widget.Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<android.widget.Button>(R.id.btnGoToSettings).setOnClickListener {
            dialog.dismiss()
            startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            )
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun loadData() {
        // Permission đã được cấp đầy đủ
    }

    // ──────────────────────────────────────────────────────
    // Setup UI
    // ──────────────────────────────────────────────────────

    private fun setUpRecent() {
        val images = MyDraftActivity.getSavedImages(this)

        if (images.isEmpty()) {
            binding.llRecent.visibility  = View.VISIBLE
            binding.rcvRecent.visibility = View.GONE
        } else {
            binding.rcvRecent.visibility = View.VISIBLE
            binding.llRecent.visibility  = View.GONE
        }

        val adapter = ImageAdapter(images)
        binding.rcvRecent.setLayoutManager(
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        )
        binding.rcvRecent.setAdapter(adapter)

        binding.tvSeeAll.setOnClickListener {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return@setOnClickListener
            mLastClickTime = SystemClock.elapsedRealtime()
            startActivity(Intent(this, MyDraftActivity::class.java))
        }
    }

    private fun setUpTemp() {
        val gson = Gson()
        val type = object : TypeToken<MutableList<Template?>?>() {}.getType()
        val temps: MutableList<Template?>? = try {
            gson.fromJson(InputStreamReader(assets.open("temp.json")), type)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        Log.e("xcncnajh1", "setUpTemp: ${temps?.size}")
        temps?.forEach { Log.e("TEMP_JSON", "image = ${it?.image}") }

        binding.rcvTemplates.setLayoutManager(
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        )
        templateList    = temps
        templateAdapter = TemplateAdapter()
        templateAdapter?.setData(templateList)
        binding.rcvTemplates.setAdapter(templateAdapter)
        binding.rcvTemplates.smoothScrollToPosition(0)
        templateAdapter?.setClickListener { _, _ -> }
    }

    private fun setupClick() {
        checkAndRequestPermission()

        binding.btnMenu.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        binding.llEdit.setOnClickListener {
            openGallery()
        }

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

    // ──────────────────────────────────────────────────────
    // Camera / Gallery / Editor
    // ──────────────────────────────────────────────────────

    private fun openCamera() {
        val file = File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraUri = androidx.core.content.FileProvider.getUriForFile(
            this, "$packageName.provider", file
        )
        cameraLauncher.launch(cameraUri)
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun openEditor(uris: List<Uri>) {
        val pages  = uris.map { PageInfo(it) }
        val intent = PhotoEditorWithBannerActivity.createIntent(this, pages)
        editorLauncher.launch(intent)
    }
}