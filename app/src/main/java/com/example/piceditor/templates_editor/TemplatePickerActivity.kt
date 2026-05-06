package com.example.piceditor.templates_editor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piceditor.R
import com.example.piceditor.SelectImageActivity
import com.example.piceditor.ads.InterAds
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityTemplatePickerBinding
import com.example.piceditor.utils.BarsUtils
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil

class TemplatePickerActivity : BaseActivityNew<ActivityTemplatePickerBinding>() {

    override fun getLayoutRes(): Int = R.layout.activity_template_picker
    override fun getFrame(): Int = 0
    override fun getDataFromIntent() {}

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

    override fun setListener() {}
    override fun initFragment(): BaseFragment<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BarsUtils.setHideNavigation(this)
        BarsUtils.setStatusBarColor(this, "#01000000".toColorInt())
        BarsUtils.setAppearanceLightStatusBars(this, true)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.rvTemplates.layoutManager = GridLayoutManager(this, 2)

        binding.rvTemplates.adapter = TemplatePickerAdapter(TemplateRepository.all) { template ->
            InterAds.showAdsBreak(this@TemplatePickerActivity) {
                val intent = Intent(this, TemplateEditorActivity::class.java)
                Log.e("xcncnajj1", "setUpTemp: " + template.id )
                intent.putExtra(TemplateEditorActivity.Companion.EXTRA_TEMPLATE_ID, template.id)
                startActivity(intent)
            }
        }
    }
}
