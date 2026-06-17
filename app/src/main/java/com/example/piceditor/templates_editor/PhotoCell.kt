package com.example.piceditor.templates_editor

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF

data class PhotoCell(
    val rect: RectF,
    // Goc xoay (do) quanh tam rect — dung cho khung anh nghieng (vd dai film cp03/sm05).
    // 0 = o thang (axis-aligned) nhu cu.
    val angle: Float = 0f,
    var bitmap: Bitmap? = null,
    val matrix: Matrix = Matrix()
)