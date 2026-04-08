package com.example.piceditor.templates_editor

import android.content.Context
import android.graphics.*
import android.view.*
import kotlin.math.max

class TemplateEditorView(context: Context) : View(context) {

    var cells: MutableList<PhotoCell> = mutableListOf()
    var templateBitmap: Bitmap? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var activeCell: PhotoCell? = null

    private var listener: ((PhotoCell) -> Unit)? = null

    fun setOnCellClickListener(l: (PhotoCell) -> Unit) {
        listener = l
    }

    // ===== gesture =====

    // ===== zoom =====
    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            activeCell?.let {
                val scale = detector.scaleFactor

                it.matrix.postScale(
                    scale,
                    scale,
                    it.rect.centerX(),
                    it.rect.centerY()
                )
                invalidate()
            }
            return true
        }
    }
    private val scaleDetector = ScaleGestureDetector(context, scaleListener)
    private var lastX = 0f
    private var lastY = 0f

    // ===== draw =====
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // draw ảnh từng cell
        cells.forEach { cell ->
            cell.bitmap?.let { bmp ->

                if (cell == activeCell) {
                    paint.style = Paint.Style.STROKE
                    paint.color = Color.WHITE
                    paint.strokeWidth = 4f
                    canvas.drawRect(cell.rect, paint)
                }

                canvas.save()

                canvas.clipRect(cell.rect)
                canvas.concat(cell.matrix)

                canvas.drawBitmap(bmp, 0f, 0f, paint)

                canvas.restore()
            }
        }

        // overlay template (khung đẹp)
        templateBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, paint)
        }
    }

    // ===== touch =====
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeCell = findCell(event.x, event.y)

                activeCell?.let {
                    listener?.invoke(it)
                }

                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_MOVE -> {
                activeCell?.let {
                    val dx = event.x - lastX
                    val dy = event.y - lastY

                    it.matrix.postTranslate(dx, dy)
                    invalidate()
                }

                lastX = event.x
                lastY = event.y
            }
        }
        return true
    }

    private fun findCell(x: Float, y: Float): PhotoCell? {
        return cells.find { it.rect.contains(x, y) }
    }

    // ===== set ảnh =====
    fun setImageToCell(cell: PhotoCell, bitmap: Bitmap) {
        cell.bitmap = bitmap

        val scale = max(
            cell.rect.width() / bitmap.width,
            cell.rect.height() / bitmap.height
        )

        val dx = cell.rect.left + (cell.rect.width() - bitmap.width * scale) / 2
        val dy = cell.rect.top + (cell.rect.height() - bitmap.height * scale) / 2

        cell.matrix.reset()
        cell.matrix.postScale(scale, scale)
        cell.matrix.postTranslate(dx, dy)

        invalidate()
    }

    // ===== export =====
    fun export(): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        draw(canvas)
        return result
    }
}