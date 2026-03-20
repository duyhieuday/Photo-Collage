package com.ezt.pdfreader.photoeditor.transform

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.ezt.pdfreader.photoeditor.data.PageState
import com.mct.doc.scanner.DocCropUtils
import java.security.MessageDigest

/**
 * @param corners corner coordinates in original image space
 * @param originalWidth effective width of original image (after rotation)
 * @param originalHeight effective height of original image (after rotation)
 */
class CropTransform(
    private val corners: IntArray,
    private val originalWidth: Int,
    private val originalHeight: Int
) : BitmapTransformation() {

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        // Scale corners if bitmap is downsampled from original
        val scaledCorners = if (originalWidth > 0 && originalHeight > 0 &&
            (toTransform.width != originalWidth || toTransform.height != originalHeight)
        ) {
            PageState.scaleCorners(
                corners, originalWidth, originalHeight,
                toTransform.width, toTransform.height
            )
        } else {
            corners
        }
        return DocCropUtils.crop(toTransform, scaledCorners)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(
            "Crop(c=${corners.contentHashCode()},ow=$originalWidth,oh=$originalHeight)"
                .toByteArray(Charsets.UTF_8)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is CropTransform) return false
        return corners.contentEquals(other.corners) &&
                originalWidth == other.originalWidth &&
                originalHeight == other.originalHeight
    }

    override fun hashCode(): Int {
        var result = corners.contentHashCode()
        result = 31 * result + originalWidth
        result = 31 * result + originalHeight
        return result
    }
}
