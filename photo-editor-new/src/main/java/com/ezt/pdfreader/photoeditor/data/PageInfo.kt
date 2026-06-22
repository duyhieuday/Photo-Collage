package com.ezt.pdfreader.photoeditor.data

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Input data for PhotoEditorActivity.
 *
 * @param uri The URI of the image
 * @param corners 8 integers representing 4 corners: [x0,y0, x1,y1, x2,y2, x3,y3]
 *                null or [-1,...] means no corners detected (will use full image bounds)
 *
 * Các tham số chỉnh sửa (rotation/flip/adjust) cho phép KHÔI PHỤC LẠI draft đúng như lúc đang chỉnh —
 * mặc định trung tính nên flow tạo mới không bị ảnh hưởng.
 */
@Parcelize
data class PageInfo(
    val uri: Uri,
    val corners: IntArray? = null,
    val filterType: FilterType = FilterType.NONE,
    val rotation: Int = 0,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 1f
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PageInfo

        if (uri != other.uri) return false
        if (filterType != other.filterType) return false
        if (rotation != other.rotation) return false
        if (flipX != other.flipX) return false
        if (flipY != other.flipY) return false
        if (brightness != other.brightness) return false
        if (contrast != other.contrast) return false
        if (saturation != other.saturation) return false
        if (warmth != other.warmth) return false
        if (corners != null) {
            if (other.corners == null) return false
            if (!corners.contentEquals(other.corners)) return false
        } else if (other.corners != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + (corners?.contentHashCode() ?: 0)
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
}
