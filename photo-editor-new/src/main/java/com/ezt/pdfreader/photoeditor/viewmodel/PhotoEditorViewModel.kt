package com.ezt.pdfreader.photoeditor.viewmodel

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.request.target.Target
import com.ezt.pdfreader.photoeditor.data.FilterType
import com.ezt.pdfreader.photoeditor.data.PageInfo
import com.ezt.pdfreader.photoeditor.data.PageState
import com.ezt.pdfreader.photoeditor.util.BitmapLoader
import com.mct.doc.scanner.DocCropUtils
import com.mct.doc.scanner.DocProcUtils
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // Events
    // ═══════════════════════════════════════════════════════════════

    private val _events = MutableSharedFlow<PhotoEditorEvent>()
    val events = _events.asSharedFlow()

    private var preciseSessionCreated = false
    private val preloadTargets = mutableListOf<Target<Bitmap>>()

    // ═══════════════════════════════════════════════════════════════
    // Initialization
    // ═══════════════════════════════════════════════════════════════

    fun initPages(pageInfos: List<PageInfo>) {
        if (_pages.value.isNotEmpty()) return

        val context = getApplication<Application>()
        viewModelScope.launch {
            val pages = withContext(Dispatchers.IO) {
                pageInfos.map { info ->
                    val (w, h) = getOriginalDimensions(context, info.uri)
                    PageState(
                        uri = info.uri,
                        originalWidth = w,
                        originalHeight = h,
                        corners = info.corners,
                        filterType = info.filterType
                    )
                }
            }
            _pages.value = pages
            pageInfos.forEach { info ->
                preloadTargets.add(
                    BitmapLoader.request(context, info.uri).override(BitmapLoader.PREVIEW_SIZE)
                        .preload()
                )
            }
            // Auto AI-crop pages that have no corners
            autoDetectCornersForNewPages()
        }
    }

    /**
     * Background AI detection for pages without corners.
     * Sets cornersWithAI so crop fragment shows AI mode.
     * Pages where detection fails remain NO_CROP (corners = null).
     */
    private fun autoDetectCornersForNewPages() {
        val pagesNeedingDetection = _pages.value.withIndex().filter { it.value.corners == null }
        if (pagesNeedingDetection.isEmpty()) return

        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                if (!preciseSessionCreated) {
                    withContext(Dispatchers.IO) { DocScanUtils.create(DocScanUtils.Mode.PRECISE) }
                    preciseSessionCreated = true
                }
                for ((index, state) in pagesNeedingDetection) {
                    val corners = withContext(Dispatchers.IO) {
                        val futureTarget = BitmapLoader.request(context, state.uri)
                            .override(BitmapLoader.AI_CROP_SIZE).submit()
                        try {
                            val bitmap = futureTarget.get()
                            val bw = bitmap.width;
                            val bh = bitmap.height
                            val result = DocScanUtils.scan(bitmap, DocScanUtils.Mode.PRECISE)
                            if (result.isNotEmpty() && result[0] >= 0)
                                PageState.scaleCorners(
                                    result,
                                    bw,
                                    bh,
                                    state.originalWidth,
                                    state.originalHeight
                                )
                            else null
                        } finally {
                            BitmapLoader.clear(context, futureTarget)
                        }
                    }
                    if (corners != null) {
                        val list = _pages.value.toMutableList()
                        if (index in list.indices && list[index].uri == state.uri) {
                            list[index] = list[index].copy().apply {
                                this.corners = corners
                                this.cornersWithAI = corners.copyOf()
                            }
                            _pages.value = list
                        }
                    }
                }
            } catch (_: Exception) {
                // Detection failed — pages remain NO_CROP
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
        if (index in 0 until _pages.value.size) _currentPageIndex.value = index
    }

    fun nextPage() {
        val next = _currentPageIndex.value + 1
        if (next < _pages.value.size) _currentPageIndex.value = next
    }

    fun previousPage() {
        val prev = _currentPageIndex.value - 1
        if (prev >= 0) _currentPageIndex.value = prev
    }

    // ═══════════════════════════════════════════════════════════════
    // Filter
    // ═══════════════════════════════════════════════════════════════

    fun applyFilter(filterType: FilterType) {
        updateCurrentPage { it.copy().apply { this.filterType = filterType } }
    }

    fun applyCurrentFilterToAllPages() {
        val current = _pages.value.getOrNull(_currentPageIndex.value) ?: return
        _pages.value =
            _pages.value.map { page -> page.copy().apply { filterType = current.filterType } }
    }

    fun getCurrentFilter(): FilterType =
        _pages.value.getOrNull(_currentPageIndex.value)?.filterType ?: FilterType.NONE

    // ═══════════════════════════════════════════════════════════════
    // Rotation & Flip
    // (PerspectiveImageView updates visually in real-time;
    //  these methods persist the new state into PageState)
    // ═══════════════════════════════════════════════════════════════

    fun setRotation(degrees: Int) {
        val normalized = ((degrees % 360) + 360) % 360
        updateCurrentPage { state ->
            state.copy().apply { this.rotation = normalized }
        }
    }

    /** Store flipX state read back from PerspectiveImageView.isFlipX(). */
    fun setFlipX(value: Boolean) {
        updateCurrentPage { state -> state.copy().apply { flipX = value } }
    }

    /** Store flipY state read back from PerspectiveImageView.isFlipY(). */
    fun setFlipY(value: Boolean) {
        updateCurrentPage { state -> state.copy().apply { flipY = value } }
    }

    fun updateCorners(corners: IntArray?) {
        updateCurrentPage { state -> state.copy().apply { this.corners = corners } }
    }

    fun setFullCrop() {
        updateCurrentPage { state -> state.copy().apply { corners = null } }
        emitEvent(PhotoEditorEvent.CornersChanged)
    }

    /** Xoá crop trên tất cả trang. */
    fun applyNoCropToAllPages() {
        _pages.value = _pages.value.map { page ->
            page.copy().apply {
                corners = null
                cornersWithAI = null
            }
        }
        // ✅ Emit CornersChanged để Fragment update UI trang hiện tại
        emitEvent(PhotoEditorEvent.CornersChanged)
    }

    fun applyCurrentCropToAllPages() {
        val current = _pages.value.getOrNull(_currentPageIndex.value) ?: return
        _pages.value = _pages.value.map { page ->
            page.copy().apply {
                corners = current.corners?.copyOf()
                cornersWithAI = current.cornersWithAI?.copyOf()
                rotation = current.rotation
                flipX = current.flipX
                flipY = current.flipY
            }
        }
    }

    /**
     * Apply AI-detected corners to all pages.
     * Uses cached [cornersWithAI] where available; fetches via DocScanUtils for the rest.
     * Emits [PhotoEditorEvent.AiCropAllDone] when finished.
     */
    fun applyAiCropToAllPages() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = getApplication<Application>()
                if (!preciseSessionCreated) {
                    withContext(Dispatchers.IO) { DocScanUtils.create(DocScanUtils.Mode.PRECISE) }
                    preciseSessionCreated = true
                }
                val updatedPages = _pages.value.toMutableList()
                updatedPages.forEachIndexed { index, state ->
                    val aiCorners: IntArray? = if (state.cornersWithAI != null) {
                        state.cornersWithAI
                    } else {
                        withContext(Dispatchers.IO) {
                            val futureTarget = BitmapLoader.request(context, state.uri)
                                .override(BitmapLoader.AI_CROP_SIZE).submit()
                            try {
                                // Scan ảnh gốc (chưa rotate) → original space
                                val bitmap = futureTarget.get()
                                val bw = bitmap.width;
                                val bh = bitmap.height
                                val result = DocScanUtils.scan(bitmap, DocScanUtils.Mode.PRECISE)
                                if (result.isNotEmpty() && result[0] >= 0)
                                    PageState.scaleCorners(
                                        result,
                                        bw,
                                        bh,
                                        state.originalWidth,
                                        state.originalHeight
                                    )
                                else null
                            } finally {
                                BitmapLoader.clear(context, futureTarget)
                            }
                        }
                    }
                    if (aiCorners != null) {
                        updatedPages[index] = state.copy().apply {
                            corners = aiCorners.copyOf()
                            cornersWithAI = aiCorners.copyOf()
                        }
                    }
                }
                _pages.value = updatedPages
                emitEvent(PhotoEditorEvent.AiCropAllDone)
            } catch (e: Exception) {
                emitEvent(PhotoEditorEvent.Error(e.message ?: "AI crop failed"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Adjust (PV values: brightness/contrast/saturation 0..2 neutral=1, warmth 0.5..2 neutral=1)
    // PerspectiveImageView handles real-time preview;
    // these methods persist after the user confirms.
    // ═══════════════════════════════════════════════════════════════

    fun setBrightness(value: Float) {
        updateCurrentPage { state -> state.copy().apply { brightness = value } }
    }

    fun setContrast(value: Float) {
        updateCurrentPage { state -> state.copy().apply { contrast = value } }
    }

    fun setSaturation(value: Float) {
        updateCurrentPage { state -> state.copy().apply { saturation = value } }
    }

    fun setWarmth(value: Float) {
        updateCurrentPage { state -> state.copy().apply { warmth = value } }
    }

    fun setAllAdjust(brightness: Float, contrast: Float, saturation: Float, warmth: Float) {
        updateCurrentPage { state ->
            state.copy().apply {
                this.brightness = brightness
                this.contrast = contrast
                this.saturation = saturation
                this.warmth = warmth
            }
        }
    }

    fun resetAdjustments() {
        updateCurrentPage { state ->
            state.copy().apply {
                brightness = 1f; contrast = 1f; saturation = 1f; warmth = 1f
            }
        }
        emitEvent(PhotoEditorEvent.AdjustChanged)
    }

    fun applyCurrentAdjustToAllPages() {
        val current = _pages.value.getOrNull(_currentPageIndex.value) ?: return
        _pages.value = _pages.value.map { page ->
            page.copy().apply {
                brightness = current.brightness
                contrast = current.contrast
                saturation = current.saturation
                warmth = current.warmth
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // AI Crop Detection
    // ═══════════════════════════════════════════════════════════════

    fun detectCornersWithAI() {
        val currentState = _pages.value.getOrNull(_currentPageIndex.value) ?: return

        // Dùng cache nếu có
        if (currentState.cornersWithAI != null) {
            updateCurrentPage { state -> state.copy().apply { corners = cornersWithAI?.copyOf() } }
            emitEvent(PhotoEditorEvent.CornersChanged)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = getApplication<Application>()
                val corners = withContext(Dispatchers.IO) {
                    if (!preciseSessionCreated) {
                        DocScanUtils.create(DocScanUtils.Mode.PRECISE)
                        preciseSessionCreated = true
                    }
                    val futureTarget = BitmapLoader.request(context, currentState.uri)
                        .override(BitmapLoader.AI_CROP_SIZE).submit()
                    try {
                        // Scan ảnh GỐC (chưa rotate) → corners trả về luôn trong original bitmap space
                        val bitmap = futureTarget.get()
                        val bw = bitmap.width;
                        val bh = bitmap.height
                        val result = DocScanUtils.scan(bitmap, DocScanUtils.Mode.PRECISE)
                        if (result.isNotEmpty() && result[0] >= 0)
                            PageState.scaleCorners(
                                result,
                                bw,
                                bh,
                                currentState.originalWidth,
                                currentState.originalHeight
                            )
                        else null
                    } finally {
                        BitmapLoader.clear(context, futureTarget)
                    }
                }
                if (corners != null) {
                    updateCurrentPage { state ->
                        state.copy()
                            .apply { this.corners = corners; cornersWithAI = corners.copyOf() }
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

    // ═══════════════════════════════════════════════════════════════
    // Page Management
    // ═══════════════════════════════════════════════════════════════

    fun deletePage(index: Int) {
        val pages = _pages.value.toMutableList()
        if (pages.size <= 1) {
            emitEvent(PhotoEditorEvent.CannotDeleteLastPage); return
        }
        pages.removeAt(index)
        _pages.value = pages
        if (_currentPageIndex.value >= pages.size) _currentPageIndex.value = pages.size - 1
        emitEvent(PhotoEditorEvent.PageDeleted(index))
    }

    fun requestDeleteCurrentPage() {
        emitEvent(PhotoEditorEvent.ConfirmDeletePage(_currentPageIndex.value))
    }

    // ═══════════════════════════════════════════════════════════════
    // Save — uses DocProcUtils for rotate/flip/adjust
    // ═══════════════════════════════════════════════════════════════

    suspend fun saveEditedPages(onProgress: ((current: Int, total: Int) -> Unit)? = null): List<Uri> =
        withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            val cacheDir = File(context.cacheDir, "edited_pages").also { it.mkdirs() }
            val pageList = _pages.value

            pageList.mapIndexed { index, state ->
                var bitmap = BitmapLoader.loadCopy(context, state.uri)

                // 1. Crop trước — corners lưu trong original (pre-rotation) space
                state.corners?.let { corners ->
                    val old = bitmap
                    bitmap = DocCropUtils.crop(bitmap, corners)
                    if (old !== bitmap) old.recycle()
                }

                // 2. GPU Filter (before rotation to match preview: Crop → Filter → Rotate/Flip/Adjust)
                if (state.filterType != FilterType.NONE) {
                    val gpuFilter = state.filterType.createGPUFilter(context)
                    gpuFilter.isThumbnail = false
                    gpuFilter.setBitmap(bitmap)
                    gpuFilter.getRawBitmap()?.let { filtered ->
                        if (bitmap !== filtered) bitmap.recycle()
                        bitmap = filtered
                    }
                }

                // 3. Rotate + flip + adjust — DocProcUtils xử lý tất cả cùng lúc
                val needsProc = state.rotation != 0 || state.flipX || state.flipY ||
                        state.brightness != 1f || state.contrast != 1f ||
                        state.saturation != 1f || state.warmth != 1f
                if (needsProc) {
                    val old = bitmap
                    bitmap = DocProcUtils.process(
                        bitmap,
                        state.rotation,
                        if (state.flipX) 1 else 0,
                        if (state.flipY) 1 else 0,
                        state.contrast, state.brightness, state.saturation, state.warmth
                    )
                    if (old !== bitmap) old.recycle()
                }

                // 5. Save
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "page_${index}_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoCollage")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out!!)
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentValues.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                    resolver.update(it, contentValues, null, null)
                }

                bitmap.recycle()

                withContext(Dispatchers.Main) { onProgress?.invoke(index + 1, pageList.size) }

                uri!!
            }
        }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════

    private fun updateCurrentPage(transform: (PageState) -> PageState) {
        val index = _currentPageIndex.value
        val list = _pages.value.toMutableList()
        if (index in list.indices) {
            list[index] = transform(list[index]); _pages.value = list
        }
    }

    private fun emitEvent(event: PhotoEditorEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    private fun getOriginalDimensions(context: android.content.Context, uri: Uri): Pair<Int, Int> {
        val cr = context.contentResolver
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var w = opts.outWidth;
        var h = opts.outHeight
        try {
            cr.openInputStream(uri)?.use { stream ->
                val o = ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                if (o == ExifInterface.ORIENTATION_ROTATE_90 || o == ExifInterface.ORIENTATION_ROTATE_270 ||
                    o == ExifInterface.ORIENTATION_TRANSPOSE || o == ExifInterface.ORIENTATION_TRANSVERSE
                ) {
                    val tmp = w; w = h; h = tmp
                }
            }
        } catch (_: Exception) {
        }
        return Pair(w, h)
    }

    override fun onCleared() {
        super.onCleared()
        if (preciseSessionCreated) {
            DocScanUtils.destroy(DocScanUtils.Mode.PRECISE); preciseSessionCreated = false
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Events
// ═══════════════════════════════════════════════════════════════

sealed class PhotoEditorEvent {
    data object CornersChanged : PhotoEditorEvent()
    data object AdjustChanged : PhotoEditorEvent()
    data object AiCropAllDone : PhotoEditorEvent()
    data class ConfirmDeletePage(val index: Int) : PhotoEditorEvent()
    data class PageDeleted(val index: Int) : PhotoEditorEvent()
    data object CannotDeleteLastPage : PhotoEditorEvent()
    data class Error(val message: String) : PhotoEditorEvent()
}
