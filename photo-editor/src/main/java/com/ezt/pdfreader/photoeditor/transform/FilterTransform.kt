package com.ezt.pdfreader.photoeditor.transform

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.ezt.pdfreader.photoeditor.data.FilterType
import java.security.MessageDigest

class FilterTransform(
    private val filterType: FilterType,
    appContext: Context
) : BitmapTransformation() {

    private val appContext: Context = appContext.applicationContext

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        if (filterType == FilterType.NONE) return toTransform

        val gpuFilter = filterType.createGPUFilter(appContext)
        gpuFilter.isThumbnail = false
        gpuFilter.setBitmap(toTransform)
        return gpuFilter.getRawBitmap() ?: toTransform
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("BrunoFilter(t=${filterType.ordinal})".toByteArray(Charsets.UTF_8))
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FilterTransform) return false
        return filterType == other.filterType
    }

    override fun hashCode(): Int = filterType.hashCode()
}
