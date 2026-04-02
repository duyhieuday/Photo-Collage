package com.example.piceditor.model

import android.net.Uri

data class ImageModel(
    val uri: Uri,
    val name: String,
    val date: Long
)