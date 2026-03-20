package com.ezt.pdfreader.photoeditor.viewmodel

import android.app.Application
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ezt.pdfreader.photoeditor.data.EditMode
import com.ezt.pdfreader.photoeditor.data.FilterType
import com.ezt.pdfreader.photoeditor.data.PageInfo
import com.ezt.pdfreader.photoeditor.data.PageState
import com.bumptech.glide.request.target.Target
import com.ezt.pdfreader.photoeditor.util.BitmapLoader
import com.mct.doc.scanner.DocCropUtils
import com.mct.doc.scanner.DocScanUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PhotoEditorViewModel(application: Application) : AndroidViewModel(application) {

    // ═══════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════

    private val _pages = MutableStateFlow<List<PageState>>(emptyList())
    val pages: StateFlow<List<PageState>> = _pages.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _editMode = MutableStateFlow(EditMode.PREVIEW)
    val editMode: StateFlow<EditMode> = _editMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // Events
    // ═══════════════════════════════════════════════════════════════

    private val _events = MutableSharedFlow<PhotoEditorEvent>()
    val events = _events.asSharedFlow()

    // Backup state for cancel operations
    private var backupState: PageState? = null

    // Track if PRECISE session has been created
    private var preciseSessionCreated = false

    // Track preloaded Glide targets for targeted cleanup
    private val preloadTargets = mutableListOf<Target<Bitmap>>()

    // ═══════════════════════════════════════════════════════════════
    // Initialization
    // ═══════════════════════════════════════════════════════════════

    fun initPages(pageInfos: List<PageInfo>) {
        if (_pages.value.isNotEmpty()) return // Already initialized

        val context = getApplication<Application>()
        viewModelScope.launch {
            val pages = withContext(Dispatchers.IO) {
                pageInfos.map { info ->
                    val (w, h) = getOriginalDimensions(context, info.uri)
                    PageState(
                        uri = info.uri,
                        originalWidth = w,
                        originalHeight = h,
                        corners = info.corners
                    )
                }
            }
            _pages.value = pages

            // Preload bitmaps at screen width into Glide cache
            pageInfos.forEach { info ->
                preloadTargets.add(
                    BitmapLoader.request(context, info.uri)
                        .override(BitmapLoader.PREVIEW_SIZE)
                        .preload()
                )
            }
        }
    }

    fun clearGlideCache() {
        val context = getApplication<Application>()
        preloadTargets.forEach { BitmapLoader.clear(context, it) }
        preloadTargets.clear()
    }

    // ═══════════════════════════════════════════════════════════════
    // Page Navigation
    // ═══════════════════════════════════════════════════════════════

    fun setCurrentPage(index: Int) {
        if (index in 0 until _pages.value.size) {
            _currentPageIndex.value = index
        }
    }

    fun nextPage() {
        val nextIndex = _currentPageIndex.value + 1
        if (nextIndex < _pages.value.size) {
            _currentPageIndex.value = nextIndex
        }
    }

    fun previousPage() {
        val prevIndex = _currentPageIndex.value - 1
        if (prevIndex >= 0) {
            _currentPageIndex.value = prevIndex
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Edit Mode
    // ═══════════════════════════════════════════════════════════════

    fun setEditMode(mode: EditMode) {
        if (mode != EditMode.PREVIEW && _editMode.value == EditMode.PREVIEW) {
            // Entering edit mode - backup current state
            backupState = _pages.value.getOrNull(_currentPageIndex.value)?.copy()
        }
        _editMode.value = mode
    }

    /**
     * @return true if state was changed, false if nothing changed
     */
    fun cancelCurrentEdit(): Boolean {
        var changed = false
        backupState?.let { backup ->
            val current = _pages.value.getOrNull(_currentPageIndex.value)
            if (current != backup) {
                updateCurrentPage { backup.copy() }
                changed = true
            }
        }
        backupState = null
        _editMode.value = EditMode.PREVIEW
        return changed
    }

    fun confirmCurrentEdit() {
        backupState = null
        _editMode.value = EditMode.PREVIEW
    }

    // ═══════════════════════════════════════════════════════════════
    // Filter Operations
    // ═══════════════════════════════════════════════════════════════

    fun applyFilter(filterType: FilterType) {
        updateCurrentPage { state ->
            state.copy().apply {
                this.filterType = filterType
            }
        }
    }

    fun getCurrentFilter(): FilterType {
        return _pages.value.getOrNull(_currentPageIndex.value)?.filterType ?: FilterType.NONE
    }

    // ═══════════════════════════════════════════════════════════════
    // Crop Operations
    // ═══════════════════════════════════════════════════════════════

    fun updateCorners(corners: IntArray?) {
        updateCurrentPage { state ->
            state.copy().apply {
                this.corners = corners
            }
        }
    }

    fun rotateLeft() {
        val currentState = _pages.value.getOrNull(_currentPageIndex.value) ?: return
        val (effectiveW, effectiveH) = currentState.getEffectiveDimensions()
        updateCurrentPage { state ->
            state.copy().apply {
                this.rotation = (rotation - 90 + 360) % 360
                if (effectiveW > 0 && effectiveH > 0) {
                    this.corners = state.corners?.let {
                        rotateCornersArray(it, effectiveW, effectiveH, clockwise = false)
                    }
                    this.cornersWithAI = state.cornersWithAI?.let {
                        rotateCornersArray(it, effectiveW, effectiveH, clockwise = false)
                    }
                } else {
                    this.corners = null
                    this.cornersWithAI = null
                }
            }
        }
        emitEvent(PhotoEditorEvent.RotationChanged(clockwise = false))
    }

    fun rotateRight() {
        val currentState = _pages.value.getOrNull(_currentPageIndex.value) ?: return
        val (effectiveW, effectiveH) = currentState.getEffectiveDimensions()
        updateCurrentPage { state ->
            state.copy().apply {
                this.rotation = (rotation + 90) % 360
                if (effectiveW > 0 && effectiveH > 0) {
                    this.corners = state.corners?.let {
                        rotateCornersArray(it, effectiveW, effectiveH, clockwise = true)
                    }
                    this.cornersWithAI = state.cornersWithAI?.let {
                        rotateCornersArray(it, effectiveW, effectiveH, clockwise = true)
                    }
                } else {
                    this.corners = null
                    this.cornersWithAI = null
                }
            }
        }
        emitEvent(PhotoEditorEvent.RotationChanged(clockwise = true))
    }

    /**
     * Get EXIF-corrected original dimensions (without rotation).
     * Only reads headers, doesn't decode pixel data.
     */
    private fun getOriginalDimensions(context: android.content.Context, uri: Uri): Pair<Int, Int> {
        val contentResolver = context.contentResolver

        // Read raw dimensions
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        var width = options.outWidth
        var height = options.outHeight

        // Apply EXIF orientation
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                    orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                    orientation == ExifInterface.ORIENTATION_TRANSVERSE
                ) {
                    val tmp = width
                    width = height
                    height = tmp
                }
            }
        } catch (_: Exception) { }

        return Pair(width, height)
    }

    /**
     * Rotate corners array by 90 degrees
     * Corners format: [x1,y1, x2,y2, x3,y3, x4,y4] for A(top-left), B(top-right), C(bottom-right), D(bottom-left)
     */
    private fun rotateCornersArray(
        corners: IntArray,
        imageWidth: Int,
        imageHeight: Int,
        clockwise: Boolean
    ): IntArray {
        if (corners.size != 8) return corners

        val result = IntArray(8)

        if (clockwise) {
            // 90° clockwise: (x, y) -> (height - 1 - y, x)
            // Corner reordering: new[A,B,C,D] = old[D,A,B,C]

            // new A = rotated old D
            result[0] = imageHeight - 1 - corners[7]
            result[1] = corners[6]
            // new B = rotated old A
            result[2] = imageHeight - 1 - corners[1]
            result[3] = corners[0]
            // new C = rotated old B
            result[4] = imageHeight - 1 - corners[3]
            result[5] = corners[2]
            // new D = rotated old C
            result[6] = imageHeight - 1 - corners[5]
            result[7] = corners[4]
        } else {
            // 90° counter-clockwise: (x, y) -> (y, width - 1 - x)
            // Corner reordering: new[A,B,C,D] = old[B,C,D,A]

            // new A = rotated old B
            result[0] = corners[3]
            result[1] = imageWidth - 1 - corners[2]
            // new B = rotated old C
            result[2] = corners[5]
            result[3] = imageWidth - 1 - corners[4]
            // new C = rotated old D
            result[4] = corners[7]
            result[5] = imageWidth - 1 - corners[6]
            // new D = rotated old A
            result[6] = corners[1]
            result[7] = imageWidth - 1 - corners[0]
        }

        return result
    }

    fun detectCornersWithAI() {
        val currentState = _pages.value.getOrNull(_currentPageIndex.value) ?: return

        // If AI corners already cached, just copy to main corners
        if (currentState.cornersWithAI != null) {
            val cachedCorners = currentState.cornersWithAI?.copyOf()
            updateCurrentPage { state ->
                state.copy().apply {
                    this.corners = cachedCorners
                }
            }
            emitEvent(PhotoEditorEvent.CornersChanged)
            return
        }

        // Fetch new corners from AI
        val rotation = currentState.rotation
        val (effectiveOrigW, effectiveOrigH) = currentState.getEffectiveDimensions()
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = getApplication<Application>()
                val corners = withContext(Dispatchers.IO) {
                    // Create PRECISE session if not already created
                    if (!preciseSessionCreated) {
                        DocScanUtils.create(DocScanUtils.Mode.PRECISE)
                        preciseSessionCreated = true
                    }

                    // Load at AI_CROP_SIZE (1440p) instead of full resolution
                    val futureTarget = BitmapLoader.request(context, currentState.uri)
                        .override(BitmapLoader.AI_CROP_SIZE)
                        .submit()
                    try {
                        val decodedBitmap = futureTarget.get()

                        // Rotate bitmap if needed before detection
                        val bitmap = if (rotation != 0) {
                            rotateBitmap(decodedBitmap, rotation.toFloat())
                        } else {
                            decodedBitmap
                        }

                        val bitmapW = bitmap.width
                        val bitmapH = bitmap.height
                        val result = DocScanUtils.scan(bitmap, DocScanUtils.Mode.PRECISE)
                        if (bitmap !== decodedBitmap) bitmap.recycle()

                        if (result.isNotEmpty() && result[0] >= 0) {
                            // Scale corners from AI bitmap space to original space
                            PageState.scaleCorners(
                                result, bitmapW, bitmapH,
                                effectiveOrigW, effectiveOrigH
                            )
                        } else null
                    } finally {
                        BitmapLoader.clear(context, futureTarget)
                    }
                }

                if (corners != null) {
                    updateCurrentPage { state ->
                        state.copy().apply {
                            this.corners = corners
                            this.cornersWithAI = corners.copyOf()
                        }
                    }
                    emitEvent(PhotoEditorEvent.CornersChanged)
                } else {
                    emitEvent(PhotoEditorEvent.Error("Failed to detect document corners"))
                }
            } catch (e: Exception) {
                emitEvent(PhotoEditorEvent.Error(e.message ?: "Failed to detect corners"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setFullCrop() {
        updateCurrentPage { state ->
            state.copy().apply {
                this.corners = null
            }
        }
        emitEvent(PhotoEditorEvent.CornersChanged)
    }

    // ═══════════════════════════════════════════════════════════════
    // Adjust Operations
    // ═══════════════════════════════════════════════════════════════

    fun setBrightness(value: Float) {
        updateCurrentPage { state ->
            state.copy().apply {
                this.brightness = value
            }
        }
        // Adjust uses ColorMatrix - no need to regenerate bitmap
        emitEvent(PhotoEditorEvent.AdjustChanged)
    }

    fun setContrast(value: Float) {
        updateCurrentPage { state ->
            state.copy().apply {
                this.contrast = value
            }
        }
        emitEvent(PhotoEditorEvent.AdjustChanged)
    }

    fun setSharpen(value: Float) {
        updateCurrentPage { state ->
            state.copy().apply {
                this.sharpen = value
            }
        }
        emitEvent(PhotoEditorEvent.AdjustChanged)
    }

    fun resetAdjustments() {
        updateCurrentPage { state ->
            state.copy().apply {
                this.brightness = 0f
                this.contrast = 0f
                this.sharpen = 0f
            }
        }
        emitEvent(PhotoEditorEvent.AdjustChanged)
    }

    // ═══════════════════════════════════════════════════════════════
    // Page Management
    // ═══════════════════════════════════════════════════════════════

    fun deletePage(index: Int) {
        val currentPages = _pages.value.toMutableList()
        if (currentPages.size <= 1) {
            emitEvent(PhotoEditorEvent.CannotDeleteLastPage)
            return
        }

        currentPages.removeAt(index)
        _pages.value = currentPages

        // Adjust current page index if needed
        if (_currentPageIndex.value >= currentPages.size) {
            _currentPageIndex.value = currentPages.size - 1
        }

        emitEvent(PhotoEditorEvent.PageDeleted(index))
    }

    fun requestDeleteCurrentPage() {
        emitEvent(PhotoEditorEvent.ConfirmDeletePage(_currentPageIndex.value))
    }

    // ═══════════════════════════════════════════════════════════════
    // Result
    // ═══════════════════════════════════════════════════════════════

    suspend fun saveEditedPages(onProgress: ((current: Int, total: Int) -> Unit)? = null): List<Uri> =
        withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            val cacheDir = File(context.cacheDir, "edited_pages")
            cacheDir.mkdirs()

            val pageList = _pages.value
            pageList.mapIndexed { index, state ->
                val hasEdits = state.rotation != 0 ||
                        state.corners != null ||
                        state.filterType != FilterType.NONE ||
                        state.brightness != 0f ||
                        state.contrast != 0f ||
                        state.sharpen != 0f

                if (!hasEdits) {
                    return@mapIndexed state.uri
                }

                var bitmap = BitmapLoader.loadCopy(context, state.uri)

                // 1. Rotate
                if (state.rotation != 0) {
                    val old = bitmap
                    bitmap = rotateBitmap(bitmap, state.rotation.toFloat())
                    if (old !== bitmap) old.recycle()
                }

                // 2. Crop
                state.corners?.let { corners ->
                    val old = bitmap
                    bitmap = DocCropUtils.crop(bitmap, corners)
                    if (old !== bitmap) old.recycle()
                }

                // 3. GPU Filter
                if (state.filterType != FilterType.NONE) {
                    val gpuFilter = state.filterType.createGPUFilter(context)
                    gpuFilter.isThumbnail = false
                    gpuFilter.setBitmap(bitmap)
                    gpuFilter.getRawBitmap()?.let { filtered ->
                        if (bitmap !== filtered) bitmap.recycle()
                        bitmap = filtered
                    }
                }

                // 4. Adjust (brightness/contrast/sharpen)
                state.getColorMatrixColorFilter()?.let { colorFilter ->
                    val old = bitmap
                    bitmap = createBitmap(
                        bitmap.width,
                        bitmap.height,
                        bitmap.config ?: Bitmap.Config.ARGB_8888
                    )
                    Canvas(bitmap).drawBitmap(
                        old,
                        0f,
                        0f,
                        Paint().apply { this.colorFilter = colorFilter })
                    old.recycle()
                }

                // 5. Save to cache
                val file = File(cacheDir, "page_${index}_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
                bitmap.recycle()

                withContext(Dispatchers.Main) {
                    onProgress?.invoke(index + 1, pageList.size)
                }

                Uri.fromFile(file)
            }
        }

    // ═══════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════

    private fun updateCurrentPage(transform: (PageState) -> PageState) {
        val index = _currentPageIndex.value
        val currentPages = _pages.value.toMutableList()
        if (index in currentPages.indices) {
            currentPages[index] = transform(currentPages[index])
            _pages.value = currentPages
        }
    }

    private fun emitEvent(event: PhotoEditorEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onCleared() {
        super.onCleared()
        // Destroy PRECISE session if it was created
        if (preciseSessionCreated) {
            DocScanUtils.destroy(DocScanUtils.Mode.PRECISE)
            preciseSessionCreated = false
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Events
// ═══════════════════════════════════════════════════════════════

sealed class PhotoEditorEvent {
    data class RotationChanged(val clockwise: Boolean) : PhotoEditorEvent()
    data object CornersChanged : PhotoEditorEvent()
    data object AdjustChanged : PhotoEditorEvent()
    data class ConfirmDeletePage(val index: Int) : PhotoEditorEvent()
    data class PageDeleted(val index: Int) : PhotoEditorEvent()
    data object CannotDeleteLastPage : PhotoEditorEvent()
    data class Error(val message: String) : PhotoEditorEvent()
}
