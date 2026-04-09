package com.example.piceditor.templates_editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.*
import kotlin.math.abs
import kotlin.math.max

class TemplateEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var cells: MutableList<PhotoCell> = mutableListOf()

    // Raw bitmap (để vẽ nền khi maskMode = WHITE)
    var templateBitmapRaw: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }

    // Mask bitmap (vùng trong suốt = chỗ hiện ảnh)
    var templateMaskBitmap: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }

    // Deprecated - giữ lại để backward compatible với code cũ
    var templateBitmap: Bitmap?
        get() = templateBitmapRaw
        set(value) { templateBitmapRaw = value }

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var activeCell: PhotoCell? = null
        private set

    private var listener: ((PhotoCell) -> Unit)? = null
    fun setOnCellClickListener(l: (PhotoCell) -> Unit) { listener = l }

    // ===== Zoom =====

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            activeCell?.let {
                it.matrix.postScale(
                    detector.scaleFactor, detector.scaleFactor,
                    it.rect.centerX(), it.rect.centerY()
                )
                invalidate()
            }
            return true
        }
    }
    private val scaleDetector = ScaleGestureDetector(context, scaleListener)
    private var lastX = 0f
    private var lastY = 0f
    private var isTap = false

    // ===== Draw =====

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Bước 1: Nền trắng
        canvas.drawColor(Color.WHITE)

        // Bước 2: Vẽ raw template (background của template, ví dụ ảnh vinyl, sky...)
        templateBitmapRaw?.let { canvas.drawBitmap(it, 0f, 0f, cellPaint) }

        // Bước 3: Vẽ ảnh vào từng cell
        cells.forEach { cell ->
            cell.bitmap?.let { bmp ->
                canvas.save()
                canvas.clipRect(cell.rect)
                canvas.translate(cell.rect.left, cell.rect.top)
                canvas.concat(cell.matrix)
                canvas.drawBitmap(bmp, 0f, 0f, cellPaint)
                canvas.restore()
            } ?: run {
                // Placeholder
                val p = Paint().apply { color = 0x44888888; style = Paint.Style.FILL }
                canvas.drawRect(cell.rect, p)
                val tp = Paint().apply {
                    color = 0xAAFFFFFF.toInt()
                    textSize = 56f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText("+", cell.rect.centerX(), cell.rect.centerY() + 20f, tp)
            }
        }

        // Bước 4: Vẽ mask lên trên — che frame, lộ ảnh qua vùng transparent
        templateMaskBitmap?.let { canvas.drawBitmap(it, 0f, 0f, overlayPaint) }

        // Bước 5: Highlight cell active
        activeCell?.let { cell ->
            val hp = Paint().apply {
                color = 0x880099FF.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }
            canvas.drawRect(cell.rect, hp)
        }
    }

    // ===== Touch =====

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeCell = findCell(event.x, event.y)
                lastX = event.x; lastY = event.y; isTap = true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                if (abs(dx) > 8f || abs(dy) > 8f) isTap = false
                if (!scaleDetector.isInProgress) {
                    activeCell?.let { it.matrix.postTranslate(dx, dy); invalidate() }
                }
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_UP -> {
                if (isTap && activeCell?.bitmap == null) activeCell?.let { listener?.invoke(it) }
                isTap = false
            }
            MotionEvent.ACTION_CANCEL -> isTap = false
        }
        return true
    }

    private fun findCell(x: Float, y: Float) = cells.find { it.rect.contains(x, y) }

    // ===== Mask creation =====

    // Template có ô TRẮNG → đổi trắng thành transparent
    fun createMaskFromWhite(src: Bitmap): Bitmap {
        val w = src.width; val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            if (r > 240 && g > 240 && b > 240) pixels[i] = Color.TRANSPARENT
        }
        val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mask.setPixels(pixels, 0, w, 0, 0, w, h)
        return mask
    }

    // Template có ô ĐEN → đổi đen thành transparent
    fun createMaskFromBlack(src: Bitmap): Bitmap {
        val w = src.width; val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            if (r < 50 && g < 50 && b < 50) pixels[i] = Color.TRANSPARENT
        }
        val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mask.setPixels(pixels, 0, w, 0, 0, w, h)
        return mask
    }

    // ===== Set ảnh =====

    fun setImageToCell(cell: PhotoCell, bitmap: Bitmap) {
        Log.d("EditorView", "setImageToCell: ${bitmap.width}x${bitmap.height}")
        cell.bitmap = bitmap
        val scale = max(cell.rect.width() / bitmap.width, cell.rect.height() / bitmap.height)
        val dx = (cell.rect.width() - bitmap.width * scale) / 2f
        val dy = (cell.rect.height() - bitmap.height * scale) / 2f
        cell.matrix.reset()
        cell.matrix.postScale(scale, scale)
        cell.matrix.postTranslate(dx, dy)
        invalidate()
    }

    // ===== Export =====

    fun export(): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(result))
        return result
    }
}