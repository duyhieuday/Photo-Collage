package com.example.piceditor.templates_editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.GridLayoutManager
import com.example.piceditor.R
import com.example.piceditor.base.BaseActivityNew
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.ActivityCategoryTemplatesBinding
import com.example.piceditor.utils.BarsUtils
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil

/**
 * Màn "xem full" 1 category: toolbar (back + tên category) + grid 2 cột thumbnail.
 * Mở từ nút "see all" trên [TemplatePickerActivity].
 */
class CategoryTemplatesActivity : BaseActivityNew<ActivityCategoryTemplatesBinding>() {

    companion object {
        private const val EXTRA_TITLE = "extra_category_title"
        private const val EXTRA_IDS = "extra_template_ids"

        fun start(context: Context, title: String, templates: List<TemplateData>) {
            val intent = Intent(context, CategoryTemplatesActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putStringArrayListExtra(EXTRA_IDS, ArrayList(templates.map { it.id }))
            }
            context.startActivity(intent)
        }
    }

    override fun getLayoutRes(): Int = R.layout.activity_category_templates
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

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun setListener() {}
    override fun initFragment(): BaseFragment<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BarsUtils.setHideNavigation(this)
        BarsUtils.setStatusBarColor(this, "#01000000".toColorInt())
        BarsUtils.setAppearanceLightStatusBars(this, true)

        binding.tvCategoryTitle.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        binding.btnBack.setOnClickListener { finish() }

        // Tái dựng list template từ id (giữ đúng thứ tự đã truyền vào).
        val ids = intent.getStringArrayListExtra(EXTRA_IDS) ?: arrayListOf()
        val templates = ids.mapNotNull { TemplateRepository.findById(it) }

        binding.rvGridTemplates.layoutManager = GridLayoutManager(this, 2)
        binding.rvGridTemplates.adapter =
            TemplatePickerAdapter(templates, R.layout.item_template) { template ->
                val intent = Intent(this, TemplateEditorActivity::class.java)
                intent.putExtra(TemplateEditorActivity.EXTRA_TEMPLATE_ID, template.id)
                startActivity(intent)
            }
    }
}
