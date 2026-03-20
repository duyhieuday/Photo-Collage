package com.ezt.pdfreader.photoeditor.data

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Input data for PhotoEditorActivity
 * @param uri The URI of the image
 * @param corners 8 integers representing 4 corners: [x0,y0, x1,y1, x2,y2, x3,y3]
 *                null or [-1,...] means no corners detected (will use full image bounds)
 */
@Parcelize
data class PageInfo(
    val uri: Uri,
    val corners: IntArray? = null
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PageInfo

        if (uri != other.uri) return false
        if (corners != null) {
            if (other.corners == null) return false
            if (!corners.contentEquals(other.corners)) return false
        } else if (other.corners != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + (corners?.contentHashCode() ?: 0)
        return result
    }
}
