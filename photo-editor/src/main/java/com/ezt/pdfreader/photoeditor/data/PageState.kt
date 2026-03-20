package com.ezt.pdfreader.photoeditor.data

import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import com.ezt.pdfreader.photoeditor.effect.ImageEffects

/**
 * Internal state for each page during editing.
 *
 * Adjust effects (brightness, contrast, sharpen) are applied via ColorMatrix on ImageView.
 */
data class PageState(
    val uri: Uri,
    val originalWidth: Int = 0,   // EXIF-corrected width (before rotation)
    val originalHeight: Int = 0,  // EXIF-corrected height (before rotation)
    var corners: IntArray? = null,
    var cornersWithAI: IntArray? = null,  // Cached AI-detected corners (fetched once)
    var filterType: FilterType = FilterType.NONE,
    var rotation: Int = 0,  // 0, 90, 180, 270
    var brightness: Float = 0f,  // -100 to 100
    var contrast: Float = 0f,    // -100 to 100
    var sharpen: Float = 0f      // -100 to 100
) {

    /**
     * Effective dimensions after applying rotation.
     * Corners are stored in this coordinate space.
     */
    fun getEffectiveDimensions(): Pair<Int, Int> {
        return if (rotation == 90 || rotation == 270) {
            Pair(originalHeight, originalWidth)
        } else {
            Pair(originalWidth, originalHeight)
        }
    }

    fun getColorMatrixColorFilter(): ColorMatrixColorFilter? {
        return ImageEffects.createAdjustFilter(brightness, contrast, sharpen)
    }

    fun copy(): PageState {
        return PageState(
            uri = uri,
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            corners = corners?.copyOf(),
            cornersWithAI = cornersWithAI?.copyOf(),
            filterType = filterType,
            rotation = rotation,
            brightness = brightness,
            contrast = contrast,
            sharpen = sharpen
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PageState

        if (uri != other.uri) return false
        if (originalWidth != other.originalWidth) return false
        if (originalHeight != other.originalHeight) return false
        if (corners != null) {
            if (other.corners == null) return false
            if (!corners.contentEquals(other.corners)) return false
        } else if (other.corners != null) return false
        if (cornersWithAI != null) {
            if (other.cornersWithAI == null) return false
            if (!cornersWithAI.contentEquals(other.cornersWithAI)) return false
        } else if (other.cornersWithAI != null) return false
        if (filterType != other.filterType) return false
        if (rotation != other.rotation) return false
        if (brightness != other.brightness) return false
        if (contrast != other.contrast) return false
        if (sharpen != other.sharpen) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + originalWidth
        result = 31 * result + originalHeight
        result = 31 * result + (corners?.contentHashCode() ?: 0)
        result = 31 * result + (cornersWithAI?.contentHashCode() ?: 0)
        result = 31 * result + filterType.hashCode()
        result = 31 * result + rotation
        result = 31 * result + brightness.hashCode()
        result = 31 * result + contrast.hashCode()
        result = 31 * result + sharpen.hashCode()
        return result
    }

    companion object {
        /**
         * Scale corners from one coordinate space to another.
         * @param corners 8-element array [x0,y0, x1,y1, x2,y2, x3,y3]
         * @param fromW/fromH source coordinate space dimensions
         * @param toW/toH target coordinate space dimensions
         */
        fun scaleCorners(corners: IntArray, fromW: Int, fromH: Int, toW: Int, toH: Int): IntArray {
            if (corners.size != 8 || fromW <= 0 || fromH <= 0 || toW <= 0 || toH <= 0) return corners
            if (fromW == toW && fromH == toH) return corners
            val scaleX = toW.toFloat() / fromW
            val scaleY = toH.toFloat() / fromH
            return IntArray(8) { i ->
                if (i % 2 == 0) (corners[i] * scaleX).toInt() else (corners[i] * scaleY).toInt()
            }
        }
    }
}
