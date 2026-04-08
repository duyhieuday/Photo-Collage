package com.example.piceditor

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.example.piceditor.templates_editor.PhotoCell
import com.example.piceditor.templates_editor.TemplateEditorView
import kotlin.math.min

class TemplateEditorActivity : AppCompatActivity() {

    private lateinit var editorView: TemplateEditorView
    private var selectedCell: PhotoCell? = null

    private lateinit var templateBitmap: Bitmap

    // 👉 size gốc template
    private val TEMPLATE_W = 1080f
    private val TEMPLATE_H = 1920f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        editorView = TemplateEditorView(this)
        setContentView(editorView)

        templateBitmap = BitmapFactory.decodeResource(
            resources,
            R.drawable.template_trip
        )

        editorView.setOnCellClickListener { cell ->
            selectedCell = cell
            openGallery()
        }

        editorView.post {
            setupTemplate()
        }
    }

    // ================= TEMPLATE =================

    private fun setupTemplate() {
        val viewW = editorView.width.toFloat()
        val viewH = editorView.height.toFloat()

        // 🔥 FIT CENTER (chuẩn nhất)
        val scale = min(
            viewW / TEMPLATE_W,
            viewH / TEMPLATE_H
        )

        val newW = TEMPLATE_W * scale
        val newH = TEMPLATE_H * scale

        val dx = (viewW - newW) / 2f
        val dy = (viewH - newH) / 2f

        val scaledBitmap = Bitmap.createScaledBitmap(
            templateBitmap,
            newW.toInt(),
            newH.toInt(),
            true
        )

        editorView.templateBitmap = scaledBitmap

        // 👉 scale rect theo template
        editorView.cells = mutableListOf(
            createCell(80f, 200f, 520f, 640f, scale, dx, dy),
            createCell(80f, 700f, 520f, 1140f, scale, dx, dy),
            createCell(80f, 1200f, 520f, 1640f, scale, dx, dy),
            createCell(600f, 500f, 1040f, 940f, scale, dx, dy),
            createCell(600f, 1000f, 1040f, 1440f, scale, dx, dy)
        )

        editorView.invalidate()
    }

    private fun createCell(
        l: Float,
        t: Float,
        r: Float,
        b: Float,
        scale: Float,
        dx: Float,
        dy: Float
    ): PhotoCell {
        return PhotoCell(
            RectF(
                l * scale + dx,
                t * scale + dy,
                r * scale + dx,
                b * scale + dy
            )
        )
    }

    // ================= GALLERY =================

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return

            val bitmap =
                MediaStore.Images.Media.getBitmap(contentResolver, uri)

            selectedCell?.let {
                editorView.setImageToCell(it, bitmap)
            }
        }
    }

    // ================= EXPORT =================

    fun export(): Bitmap {
        return editorView.export()
    }
}