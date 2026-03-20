package com.ezt.pdfreader.photoeditor.data

import android.net.Uri

/**
 * Internal state for each page during editing.
 *
 * - rotation / flipX / flipY  → applied via PerspectiveImageView (real-time) and DocProcUtils (save)
 * - brightness / contrast / saturation / warmth → stored as PerspectiveImageView / DocProcUtils values directly
 *   brightness/contrast/saturation: 0..2 (neutral = 1)
 *   warmth: 0.5..2 (neutral = 1)
 */
data class PageState(
    val uri: Uri,
    val originalWidth: Int = 0,   // EXIF-corrected width (before user rotation)
    val originalHeight: Int = 0,  // EXIF-corrected height (before user rotation)
    var corners: IntArray? = null,
    var cornersWithAI: IntArray? = null,  // Cached AI-detected corners (fetched once)
    var filterType: FilterType = FilterType.NONE,
    var rotation: Int = 0,        // 0, 90, 180, 270 (user rotation on top of EXIF)
    var flipX: Boolean = false,
    var flipY: Boolean = false,
    var brightness: Float = 1f,   // 0..2, neutral = 1
    var contrast: Float = 1f,     // 0..2, neutral = 1
    var saturation: Float = 1f,   // 0..2, neutral = 1
    var warmth: Float = 1f        // 0.5..2, neutral = 1
) {

    fun copy(): PageState {
        return PageState(
            uri = uri,
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            corners = corners?.copyOf(),
            cornersWithAI = cornersWithAI?.copyOf(),
            filterType = filterType,
            rotation = rotation,
            flipX = flipX,
            flipY = flipY,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            warmth = warmth
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PageState
        if (uri != other.uri) return false
        if (originalWidth != other.originalWidth) return false
        if (originalHeight != other.originalHeight) return false
        if (!corners.contentEqualsNullable(other.corners)) return false
        if (!cornersWithAI.contentEqualsNullable(other.cornersWithAI)) return false
        if (filterType != other.filterType) return false
        if (rotation != other.rotation) return false
        if (flipX != other.flipX) return false
        if (flipY != other.flipY) return false
        if (brightness != other.brightness) return false
        if (contrast != other.contrast) return false
        if (saturation != other.saturation) return false
        if (warmth != other.warmth) return false
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
        result = 31 * result + flipX.hashCode()
        result = 31 * result + flipY.hashCode()
        result = 31 * result + brightness.hashCode()
        result = 31 * result + contrast.hashCode()
        result = 31 * result + saturation.hashCode()
        result = 31 * result + warmth.hashCode()
        return result
    }

    companion object {
        private fun IntArray?.contentEqualsNullable(other: IntArray?): Boolean {
            if (this == null && other == null) return true
            if (this == null || other == null) return false
            return contentEquals(other)
        }

        /**
         * Scale corners from one coordinate space to another.
         * @param corners 8-element array [x0,y0, x1,y1, x2,y2, x3,y3]
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
