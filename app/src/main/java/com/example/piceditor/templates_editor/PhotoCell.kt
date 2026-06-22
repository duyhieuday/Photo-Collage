package com.example.piceditor.templates_editor

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri

data class PhotoCell(
    val rect: RectF,
    // Goc xoay (do) quanh tam rect — dung cho khung anh nghieng (vd dai film cp03/sm05).
    // 0 = o thang (axis-aligned) nhu cu.
    val angle: Float = 0f,
    var bitmap: Bitmap? = null,
    val matrix: Matrix = Matrix()
) {
    // Nguồn ảnh gốc đã fill vào ô (content:// hoặc file://) — để lưu/khôi phục draft.
    // Đặt NGOÀI primary constructor nên không tính vào equals()/hashCode()/copy().
    var sourceUri: Uri? = null
}