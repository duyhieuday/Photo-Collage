package com.example.piceditor.templates_editor

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF

data class PhotoCell(
    val rect: RectF,
    var bitmap: Bitmap? = null,
    val matrix: Matrix = Matrix()
)