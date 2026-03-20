package com.ezt.pdfreader.photoeditor.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.Target

object BitmapLoader {

    /** Preview size (px) = screen width, for display in ViewPager. */
    val PREVIEW_SIZE: Int = Resources.getSystem().displayMetrics.widthPixels

    /** Max resolution (px) for AI corner detection. */
    const val AI_CROP_SIZE = 1440

    /**
     * Create a Glide request builder for loading bitmap from URI.
     * Handles EXIF orientation automatically.
     */
    fun request(context: Context, uri: Uri): RequestBuilder<Bitmap> {
        return Glide.with(context).asBitmap().load(uri)
    }

    /**
     * Clear a Glide target (release its hold on cached resource).
     */
    fun clear(context: Context, target: Target<Bitmap>) {
        Glide.with(context).clear(target)
    }

    /**
     * Load bitmap synchronously (read-only, Glide-managed).
     * Must be called from background thread.
     * The bitmap must NOT be recycled by caller.
     */
    fun <R> withBitmap(context: Context, uri: Uri, block: (Bitmap) -> R): R {
        val futureTarget = request(context, uri).submit()
        return try {
            block(futureTarget.get())
        } finally {
            clear(context, futureTarget)
        }
    }

    /**
     * Load a mutable copy of bitmap synchronously.
     * Must be called from background thread.
     * Caller owns the returned bitmap and must recycle it.
     */
    fun loadCopy(context: Context, uri: Uri): Bitmap {
        return withBitmap(context, uri) { glideBitmap ->
            glideBitmap.copy(glideBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }
    }
}
