package com.example.piceditor.templates_editor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piceditor.R

class TemplatePickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_picker)

        val recycler = findViewById<RecyclerView>(R.id.rvTemplates)
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = TemplatePickerAdapter(TemplateRepository.all) { template ->
            val intent = Intent(this, TemplateEditorActivity::class.java)
            intent.putExtra(TemplateEditorActivity.Companion.EXTRA_TEMPLATE_ID, template.id)
            startActivity(intent)
        }
    }
}
