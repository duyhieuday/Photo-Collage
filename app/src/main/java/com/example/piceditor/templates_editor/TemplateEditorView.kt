package com.example.piceditor.templates_editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.*
import com.example.piceditor.draw.DrawView
import kotlin.math.abs
import kotlin.math.max

class TemplateEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    // ── Child views ───────────────────────────────────────
    // DrawView để vẽ sticker/text lên trên template
    val drawView: DrawView? by lazy {
        try { DrawView(context) } catch (e: Exception) { null }
    }

    // ── Data ──────────────────────────────────────────────
    var cells: MutableList<PhotoCell> = mutableListOf()

    var templateBitmapRaw: Bitmap? = null
        set(value) { field = value; invalidate() }

    var templateMaskBitmap: Bitmap? = null
        set(value) { field = value; invalidate() }

    // Background riêng (set từ BackgroundAdapter)
    private var backgroundBitmap: Bitmap? = null

    // Border: spacing và corner giữa mask và ảnh
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

    // ── Layout ────────────────────────────────────────────

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        drawView?.layout(0, 0, r - l, b - t)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        drawView?.measure(widthMeasureSpec, heightMeasureSpec)
    }

    // ── Gesture state ─────────────────────────────────────
    // Xử lý drag + zoom + rotate bằng raw touch — 1 handler duy nhất, không dùng ScaleGestureDetector
    // để tránh conflict và đảm bảo mượt mà

    private var lastX = 0f
    private var lastY = 0f
    private var isTap = false

    // 2-finger state
    private var lastMidX   = 0f   // midpoint giữa 2 ngón
    private var lastMidY   = 0f
    private var lastSpan   = 0f   // khoảng cách 2 ngón
    private var lastAngle  = 0f   // góc 2 ngón

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

    // ── Kích thước cố định — không thay đổi dù view bị resize ──
    private var fixedW = 0
    private var fixedH = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // ✅ Chỉ lưu lần đầu tiên (khi panel chưa mở)
        if (fixedW == 0 && fixedH == 0 && w > 0 && h > 0) {
            fixedW = w
            fixedH = h
        }
    }

    // ── Corner radius cho view ────────────────────────────
    private val viewCornerRadius = 8f * resources.displayMetrics.density // 8dp

    override fun dispatchDraw(canvas: Canvas) {
        val w = if (fixedW > 0) fixedW.toFloat() else width.toFloat()
        val h = if (fixedH > 0) fixedH.toFloat() else height.toFloat()
        val path = Path().apply {
            addRoundRect(RectF(0f, 0f, w, h), viewCornerRadius, viewCornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(path)
        super.dispatchDraw(canvas)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = if (fixedW > 0) fixedW.toFloat() else width.toFloat()
        val h = if (fixedH > 0) fixedH.toFloat() else height.toFloat()

        val path = Path().apply {
            addRoundRect(RectF(0f, 0f, w, h), viewCornerRadius, viewCornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(path)

        // 1. Nền trắng mặc định — fill toàn bộ fixedW x fixedH
        canvas.drawColor(Color.WHITE)

        // 2. Background ảnh (nếu user chọn) — fill toàn bộ fixed size
        backgroundBitmap?.let {
            val fw = if (fixedW > 0) fixedW.toFloat() else width.toFloat()
            val fh = if (fixedH > 0) fixedH.toFloat() else height.toFloat()
            canvas.drawBitmap(it, null, RectF(0f, 0f, fw, fh), bgPaint)
        }

        // 3. Raw template — vẽ căn giữa theo dy offset
        templateBitmapRaw?.let {
            val dy = if (fixedH > 0 && it.height < fixedH)
                (fixedH - it.height) / 2f else 0f
            canvas.drawBitmap(it, 0f, dy, cellPaint)
        }

        // 4. Ảnh vào từng cell
        cells.forEach { cell ->
            // ✅ Tính drawRect theo spacing
            val drawRect = if (cellSpacing > 0f) RectF(
                cell.rect.left   + cellSpacing, cell.rect.top    + cellSpacing,
                cell.rect.right  - cellSpacing, cell.rect.bottom - cellSpacing
            ) else RectF(cell.rect)

            canvas.save()

            // ✅ Clip rounded — áp dụng cho CẢ ảnh lẫn placeholder
            if (cellCorner > 0f) {
                canvas.clipPath(Path().apply {
                    addRoundRect(drawRect, cellCorner, cellCorner, Path.Direction.CW)
                })
            } else {
                canvas.clipRect(drawRect)
            }

            cell.bitmap?.let { bmp ->
                // ✅ CenterCrop: translate về gốc drawRect rồi concat matrix
                canvas.translate(drawRect.left, drawRect.top)
                canvas.concat(cell.matrix)
                canvas.drawBitmap(bmp, 0f, 0f, cellPaint)
            } ?: run {
                // Placeholder khi chưa có ảnh
                canvas.drawRect(drawRect, Paint().apply { color = 0x44888888; style = Paint.Style.FILL })
                canvas.drawText(
                    "+", drawRect.centerX(), drawRect.centerY() + 20f,
                    Paint().apply {
                        color = 0xAAFFFFFF.toInt(); textSize = 56f
                        textAlign = Paint.Align.CENTER; isAntiAlias = true
                        typeface = Typeface.DEFAULT_BOLD
                    }
                )
            }

            canvas.restore()
        }

        // 5. Mask overlay
        templateMaskBitmap?.let { canvas.drawBitmap(it, 0f, 0f, overlayPaint) }
    }

    // ── Touch ─────────────────────────────────────────────
    // Unified handler: drag (1 ngón) + zoom+rotate (2 ngón) cùng lúc, mượt mà

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {

            // ── 1 ngón chạm xuống ──────────────────────────
            MotionEvent.ACTION_DOWN -> {
                val tappedCell = findCell(event.x, event.y)
                activeCell = tappedCell  // null nếu tap vào vùng trống
                lastX  = event.x
                lastY  = event.y
                isTap  = true
            }

            // ── Ngón thứ 2 chạm xuống ──────────────────────
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    // Snapshot trạng thái 2 ngón lúc bắt đầu
                    val (mx, my) = midPoint(event)
                    lastMidX  = mx
                    lastMidY  = my
                    lastSpan  = spacing(event)
                    lastAngle = angle(event)
                }
                isTap = false
            }

            // ── Di chuyển ──────────────────────────────────
            MotionEvent.ACTION_MOVE -> {
                val cell = activeCell ?: return true

                if (event.pointerCount == 1) {
                    // ── Drag ──
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    if (abs(dx) > 8f || abs(dy) > 8f) isTap = false
                    cell.matrix.postTranslate(dx, dy)
                    lastX = event.x
                    lastY = event.y
                    invalidate()

                } else if (event.pointerCount >= 2) {
                    // ── Zoom + Rotate + Drag 2 ngón cùng lúc ──
                    isTap = false

                    val (curMidX, curMidY) = midPoint(event)
                    val curSpan  = spacing(event)
                    val curAngle = angle(event)

                    // Scale: tỉ lệ span mới / span cũ
                    val scaleFactor = if (lastSpan > 0f) curSpan / lastSpan else 1f

                    // Rotate: delta góc
                    var dAngle = curAngle - lastAngle
                    // Normalize về [-180, 180] để tránh nhảy góc
                    if (dAngle > 180f)  dAngle -= 360f
                    if (dAngle < -180f) dAngle += 360f

                    // Drag: midpoint 2 ngón dịch chuyển
                    val dMidX = curMidX - lastMidX
                    val dMidY = curMidY - lastMidY

                    // Tâm transform = midpoint 2 ngón (trên không gian view)
                    // Cần chuyển về không gian cell (trừ cell.rect.left/top)
                    val pivotX = curMidX - cell.rect.left
                    val pivotY = curMidY - cell.rect.top

                    // Áp dụng: translate → scale → rotate, tất cả quanh pivot
                    cell.matrix.apply {
                        postTranslate(dMidX, dMidY)
                        postScale(scaleFactor, scaleFactor, pivotX, pivotY)
                        if (abs(dAngle) < 30f) {           // guard tránh giật lớn
                            postRotate(dAngle, pivotX, pivotY)
                        }
                    }

                    lastMidX  = curMidX
                    lastMidY  = curMidY
                    lastSpan  = curSpan
                    lastAngle = curAngle
                    invalidate()
                }
            }

            // ── Nhấc ngón ──────────────────────────────────
            MotionEvent.ACTION_POINTER_UP -> {
                // Khi nhấc 1 trong 2 ngón → reset về trạng thái 1 ngón
                // Lấy vị trí ngón còn lại làm lastX/lastY để drag tiếp mượt
                val remainIndex = if (event.actionIndex == 0) 1 else 0
                lastX     = event.getX(remainIndex)
                lastY     = event.getY(remainIndex)
                lastSpan  = 0f
            }

            MotionEvent.ACTION_UP -> {
                if (isTap) {
                    val cell = activeCell
                    if (cell != null && cell.bitmap == null) {
                        // Tap vào cell trống → mở gallery
                        listener?.invoke(cell)
                    }
                    // Tap vào vùng trống hoặc cell đã có ảnh → không làm gì
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
    private fun findCell(x: Float, y: Float) = cells.find { it.rect.contains(x, y) }

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

        // ✅ Tính drawRect theo spacing — giống onDraw
        val drawRect = if (cellSpacing > 0f) RectF(
            cell.rect.left   + cellSpacing, cell.rect.top    + cellSpacing,
            cell.rect.right  - cellSpacing, cell.rect.bottom - cellSpacing
        ) else RectF(cell.rect)

        // ✅ CenterCrop: scale vừa khít drawRect, căn giữa
        val scale = max(drawRect.width() / bitmap.width, drawRect.height() / bitmap.height)
        val dx = (drawRect.width()  - bitmap.width  * scale) / 2f
        val dy = (drawRect.height() - bitmap.height * scale) / 2f

        cell.matrix.reset()
        cell.matrix.postScale(scale, scale)
        cell.matrix.postTranslate(dx, dy)
        invalidate()
    }

    // ── Export ────────────────────────────────────────────

    fun export(): Bitmap {
        // ✅ Dùng fixedW/fixedH — kích thước thực khi template load lần đầu
        // Không dùng width/height vì có thể bị thay đổi khi panel tool mở
        val exportW = if (fixedW > 0) fixedW else width
        val exportH = if (fixedH > 0) fixedH else height
        val result = Bitmap.createBitmap(exportW, exportH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        draw(canvas)
        drawView?.draw(canvas)
        return result
    }
}