package com.example.piceditor.templates_editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.*
import com.example.piceditor.draw.DrawView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class TemplateEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    // ── Child views ───────────────────────────────────────
    val drawView: DrawView? by lazy {
        try { DrawView(context) } catch (e: Exception) { null }
    }

    // ── Logic space của template (set bởi activity sau khi load bitmap) ──
    var templateLogicW: Float = 1125f
    var templateLogicH: Float = 2000f

    // ── Data ──────────────────────────────────────────────
    var cells: MutableList<PhotoCell> = mutableListOf()

    var templateBitmapRaw: Bitmap? = null
        set(value) { field = value; invalidate() }

    var templateMaskBitmap: Bitmap? = null
        set(value) { field = value; invalidate() }

    private var backgroundBitmap: Bitmap? = null

    private var cellSpacing: Float = 0f
    private var cellCorner: Float  = 0f

    // ── Paint ─────────────────────────────────────────────
    private val cellPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint      = Paint(Paint.ANTI_ALIAS_FLAG)

    var activeCell: PhotoCell? = null
        private set

    private var listener: ((PhotoCell) -> Unit)? = null
    fun setOnCellClickListener(l: (PhotoCell) -> Unit) { listener = l }

    init {
        setWillNotDraw(false)
        drawView?.let { addView(it) }
    }

    // ── Layout: tự co lại đúng aspect ratio template ──────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        drawView?.measure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        drawView?.layout(0, 0, r - l, b - t)
    }

    // ── Transform: logic space → view space ──────────────
    private fun currentTransform(): FloatArray {
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        if (viewW <= 0f || viewH <= 0f) return floatArrayOf(1f, 0f, 0f)

        // ✅ FILL: scale theo cạnh lớn hơn → template fill kín view, có thể bị crop nhẹ
        val scale = max(viewW / templateLogicW, viewH / templateLogicH)
        val dx = (viewW - templateLogicW * scale) / 2f
        val dy = (viewH - templateLogicH * scale) / 2f
        return floatArrayOf(scale, dx, dy)
    }

    // ── Gesture state ─────────────────────────────────────

    private var lastX = 0f
    private var lastY = 0f
    private var isTap = false

    private var lastMidX   = 0f
    private var lastMidY   = 0f
    private var lastSpan   = 0f
    private var lastAngle  = 0f

    private fun midPoint(e: MotionEvent) = Pair(
        (e.getX(0) + e.getX(1)) / 2f,
        (e.getY(0) + e.getY(1)) / 2f
    )

    private fun spacing(e: MotionEvent): Float {
        val dx = e.getX(1) - e.getX(0)
        val dy = e.getY(1) - e.getY(0)
        return Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
    }

    private fun angle(e: MotionEvent): Float {
        val dx = (e.getX(1) - e.getX(0)).toDouble()
        val dy = (e.getY(1) - e.getY(0)).toDouble()
        return Math.toDegrees(Math.atan2(dy, dx)).toFloat()
    }

    // ── Corner radius cho view ────────────────────────────
    private val viewCornerRadius = 8f * resources.displayMetrics.density // 8dp

    override fun dispatchDraw(canvas: Canvas) {
        val path = Path().apply {
            addRoundRect(
                RectF(0f, 0f, width.toFloat(), height.toFloat()),
                viewCornerRadius, viewCornerRadius, Path.Direction.CW
            )
        }
        canvas.clipPath(path)
        super.dispatchDraw(canvas)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val path = Path().apply {
            addRoundRect(RectF(0f, 0f, w, h), viewCornerRadius, viewCornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(path)

        // 1. Nền trắng
        canvas.drawColor(Color.WHITE)

        // 2. Background ảnh — fill toàn bộ view
        backgroundBitmap?.let {
            canvas.drawBitmap(it, null, RectF(0f, 0f, w, h), bgPaint)
        }

        // ✅ Tính transform hiện tại
        val tf = currentTransform()
        val scale = tf[0]
        val dx = tf[1]
        val dy = tf[2]

        // ✅ DEBUG LOG (có thể xóa sau khi test xong)
        Log.d("TemplateDebug",
            "viewW=$w viewH=$h | logic=${templateLogicW}x${templateLogicH} | " +
                    "scale=$scale dx=$dx dy=$dy | " +
                    "rawBmp=${templateBitmapRaw?.width}x${templateBitmapRaw?.height}")

        // Vẽ template + cells + mask trong cùng 1 transform
        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)

        // 3. Template bitmap — fill logic space
        templateBitmapRaw?.let {
            canvas.drawBitmap(it, null, RectF(0f, 0f, templateLogicW, templateLogicH), cellPaint)
        }

        // 4. Cells
        cells.forEach { cell ->
            val drawRect = if (cellSpacing > 0f) RectF(
                cell.rect.left   + cellSpacing, cell.rect.top    + cellSpacing,
                cell.rect.right  - cellSpacing, cell.rect.bottom - cellSpacing
            ) else RectF(cell.rect)

            canvas.save()

            if (cellCorner > 0f) {
                canvas.clipPath(Path().apply {
                    addRoundRect(drawRect, cellCorner, cellCorner, Path.Direction.CW)
                })
            } else {
                canvas.clipRect(drawRect)
            }

            cell.bitmap?.let { bmp ->
                canvas.translate(drawRect.left, drawRect.top)
                canvas.concat(cell.matrix)
                canvas.drawBitmap(bmp, 0f, 0f, cellPaint)
            } ?: run {
                canvas.drawRect(drawRect, Paint().apply { color = 0x44888888; style = Paint.Style.FILL })
                canvas.drawText(
                    "+", drawRect.centerX(), drawRect.centerY() + 20f,
                    Paint().apply {
                        color = 0xAAFFFFFF.toInt()
                        textSize = 56f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = Typeface.DEFAULT_BOLD
                    }
                )
            }

            canvas.restore()
        }

        // 5. Mask overlay — fill logic space
        templateMaskBitmap?.let {
            canvas.drawBitmap(it, null, RectF(0f, 0f, templateLogicW, templateLogicH), overlayPaint)
        }

        canvas.restore()
    }

    // ── Touch ─────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val tf = currentTransform()
        val scale = tf[0]
        val dx = tf[1]
        val dy = tf[2]

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                val tappedCell = findCell(event.x, event.y)
                activeCell = tappedCell
                lastX  = event.x
                lastY  = event.y
                isTap  = true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    val (mx, my) = midPoint(event)
                    lastMidX  = mx
                    lastMidY  = my
                    lastSpan  = spacing(event)
                    lastAngle = angle(event)
                }
                isTap = false
            }

            MotionEvent.ACTION_MOVE -> {
                val cell = activeCell ?: return true

                if (event.pointerCount == 1) {
                    val ddx = (event.x - lastX) / scale
                    val ddy = (event.y - lastY) / scale
                    if (abs(event.x - lastX) > 8f || abs(event.y - lastY) > 8f) isTap = false
                    cell.matrix.postTranslate(ddx, ddy)
                    lastX = event.x
                    lastY = event.y
                    invalidate()

                } else if (event.pointerCount >= 2) {
                    isTap = false

                    val (curMidX, curMidY) = midPoint(event)
                    val curSpan  = spacing(event)
                    val curAngle = angle(event)

                    val scaleFactor = if (lastSpan > 0f) curSpan / lastSpan else 1f

                    var dAngle = curAngle - lastAngle
                    if (dAngle > 180f)  dAngle -= 360f
                    if (dAngle < -180f) dAngle += 360f

                    val dMidX = (curMidX - lastMidX) / scale
                    val dMidY = (curMidY - lastMidY) / scale

                    val pivotLogicX = (curMidX - dx) / scale - cell.rect.left
                    val pivotLogicY = (curMidY - dy) / scale - cell.rect.top

                    cell.matrix.apply {
                        postTranslate(dMidX, dMidY)
                        postScale(scaleFactor, scaleFactor, pivotLogicX, pivotLogicY)
                        if (abs(dAngle) < 30f) {
                            postRotate(dAngle, pivotLogicX, pivotLogicY)
                        }
                    }

                    lastMidX  = curMidX
                    lastMidY  = curMidY
                    lastSpan  = curSpan
                    lastAngle = curAngle
                    invalidate()
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val remainIndex = if (event.actionIndex == 0) 1 else 0
                lastX     = event.getX(remainIndex)
                lastY     = event.getY(remainIndex)
                lastSpan  = 0f
            }

            MotionEvent.ACTION_UP -> {
                if (isTap) {
                    val cell = activeCell
                    if (cell != null && cell.bitmap == null) {
                        listener?.invoke(cell)
                    }
                }
                isTap = false
            }

            MotionEvent.ACTION_CANCEL -> {
                isTap    = false
                lastSpan = 0f
            }
        }
        return true
    }

    // Convert touch point (view space) → logic space
    private fun findCell(x: Float, y: Float): PhotoCell? {
        val tf = currentTransform()
        val scale = tf[0]
        val dx = tf[1]
        val dy = tf[2]
        if (scale <= 0f) return null
        val logicX = (x - dx) / scale
        val logicY = (y - dy) / scale
        return cells.find { it.rect.contains(logicX, logicY) }
    }

    // ── Public API ────────────────────────────────────────

    fun setBackgroundBitmap(bitmap: Bitmap) {
        backgroundBitmap = bitmap
        invalidate()
    }

    fun setCellSpacing(spacing: Float) {
        cellSpacing = spacing
        invalidate()
    }

    fun setCellCorner(corner: Float) {
        cellCorner = corner
        invalidate()
    }

    // ── Mask creation ─────────────────────────────────────

    fun createMaskFromWhite(src: Bitmap): Bitmap {
        val w = src.width; val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val r = Color.red(pixels[i]); val g = Color.green(pixels[i]); val b = Color.blue(pixels[i])
            if (r > 240 && g > 240 && b > 240) pixels[i] = Color.TRANSPARENT
        }
        val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mask.setPixels(pixels, 0, w, 0, 0, w, h)
        return mask
    }

    fun createMaskFromBlack(src: Bitmap): Bitmap {
        val w = src.width; val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val r = Color.red(pixels[i]); val g = Color.green(pixels[i]); val b = Color.blue(pixels[i])
            if (r < 50 && g < 50 && b < 50) pixels[i] = Color.TRANSPARENT
        }
        val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mask.setPixels(pixels, 0, w, 0, 0, w, h)
        return mask
    }

    // ── Set ảnh vào cell ──────────────────────────────────
    fun setImageToCell(cell: PhotoCell, bitmap: Bitmap) {
        Log.d("TemplateEditorView", "setImageToCell: ${bitmap.width}x${bitmap.height}")
        cell.bitmap = bitmap

        val drawRect = if (cellSpacing > 0f) RectF(
            cell.rect.left   + cellSpacing, cell.rect.top    + cellSpacing,
            cell.rect.right  - cellSpacing, cell.rect.bottom - cellSpacing
        ) else RectF(cell.rect)

        val s = max(drawRect.width() / bitmap.width, drawRect.height() / bitmap.height)
        val dx = (drawRect.width()  - bitmap.width  * s) / 2f
        val dy = (drawRect.height() - bitmap.height * s) / 2f

        cell.matrix.reset()
        cell.matrix.postScale(s, s)
        cell.matrix.postTranslate(dx, dy)
        invalidate()
    }

    // ── Kiểm tra fill ảnh ─────────────────────────────────
    /** Số ô ảnh chưa được fill (bitmap null). */
    fun getEmptyCellCount(): Int = cells.count { it.bitmap == null }

    /** True nếu tất cả ô ảnh đều đã có ảnh. */
    fun areAllCellsFilled(): Boolean = cells.all { it.bitmap != null }

    // ── Export ────────────────────────────────────────────
    fun export(): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        // Ẩn khung điều khiển (xoá, copy, scale, xoay) của sticker/text khi render ảnh lưu.
        // draw(canvas) cũng vẽ drawView (child) nên phải bật cờ TRƯỚC cả 2 lần vẽ.
        drawView?.drawManager?.setStickerForceHideControls(true)
        try {
            draw(canvas)
            drawView?.draw(canvas)
        } finally {
            drawView?.drawManager?.setStickerForceHideControls(false)
        }
        return result
    }
}