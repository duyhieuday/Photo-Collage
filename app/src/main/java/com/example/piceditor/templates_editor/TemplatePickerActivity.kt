package com.example.piceditor.templates_editor

import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piceditor.R
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityTemplatePickerBinding
import com.example.piceditor.utils.BarsUtils

class TemplatePickerActivity : BaseActivityNew<ActivityTemplatePickerBinding>() {

    override fun getLayoutRes(): Int = R.layout.activity_template_picker
    override fun getFrame(): Int = 0
    override fun getDataFromIntent() {}
    override fun doAfterOnCreate() {}
    override fun setListener() {}
    override fun initFragment(): BaseFragment<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BarsUtils.setHideNavigation(this)
        BarsUtils.setStatusBarColor(this, "#01000000".toColorInt())
        BarsUtils.setAppearanceLightStatusBars(this, true)

        binding.rvTemplates.layoutManager = GridLayoutManager(this, 2)

        binding.rvTemplates.adapter = TemplatePickerAdapter(TemplateRepository.all) { template ->
            val intent = Intent(this, TemplateEditorActivity::class.java)
            intent.putExtra(TemplateEditorActivity.Companion.EXTRA_TEMPLATE_ID, template.id)
            startActivity(intent)
        }
    }
}
