package com.ezt.pdfreader.photoeditor.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Watermark cho ảnh export của user FREE (đòn bẩy chuyển đổi: "bỏ watermark" = lý do mua Premium).
 * - User Premium / đã RemoveAd  -> trả về bitmap GỐC (không watermark).
 * - Mọi lỗi đều fallback bitmap gốc  -> KHÔNG bao giờ làm hỏng chức năng lưu.
 *
 * Đặt trong module photo-editor-new để CẢ app module lẫn module này gọi được.
 * Check premium đọc thẳng SharedPreferences mà com.example.piceditor.ads.Prefs dùng
 * (tên = string app_name, key "Premium"/"RemoveAd") để không phụ thuộc class của app module.
 */
object WatermarkUtil {

    private const val WATERMARK_TEXT = "Photo Collage"

    private fun isPremium(context: Context): Boolean {
        return try {
            val id = context.resources.getIdentifier("app_name", "string", context.packageName)
            val prefsName = if (id != 0) context.getString(id) else context.packageName
            val sp = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            sp.getInt("Premium", 0) == 1 || sp.getBoolean("RemoveAd", false)
        } catch (e: Exception) {
            false
        }
    }

    /** Trả về bản sao có watermark cho FREE; bitmap gốc cho Premium hoặc khi có lỗi. */
    fun applyIfFree(context: Context, src: Bitmap): Bitmap {
        return try {
            if (isPremium(context)) return src
            // Luôn vẽ trên BẢN SAO để không ảnh hưởng bitmap gốc (có thể đang được tái dùng cho share/draft).
            val out = src.copy(Bitmap.Config.ARGB_8888, true) ?: return src
            val canvas = Canvas(out)
            val w = out.width.toFloat()
            val h = out.height.toFloat()
            val textSize = (minOf(w, h) * 0.045f).coerceIn(26f, 96f)
            val pad = textSize * 0.7f

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                this.textSize = textSize
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(textSize * 0.14f, 0f, textSize * 0.04f, Color.argb(170, 0, 0, 0))
                alpha = 205
            }
            val textWidth = paint.measureText(WATERMARK_TEXT)
            val x = w - textWidth - pad
            val y = h - pad
            canvas.drawText(WATERMARK_TEXT, x, y, paint)
            out
        } catch (e: Exception) {
            src
        }
    }
}
