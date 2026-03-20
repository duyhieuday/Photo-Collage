package com.ezt.pdfreader.photoeditor.transform

import android.graphics.Bitmap
import android.graphics.Matrix
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class RotateTransform(private val rotation: Int) : BitmapTransformation() {

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        if (rotation == 0) return toTransform
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        return Bitmap.createBitmap(toTransform, 0, 0, toTransform.width, toTransform.height, matrix, true)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("Rotate(r=$rotation)".toByteArray(Charsets.UTF_8))
    }

    override fun equals(other: Any?): Boolean {
        if (other !is RotateTransform) return false
        return rotation == other.rotation
    }

    override fun hashCode(): Int = rotation
}
