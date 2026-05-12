package com.example.piceditor.sever.ai_remove_bg.model

import com.example.piceditor.sever.ai_remove_bg.Url
import com.google.gson.annotations.SerializedName

class Media {
    @SerializedName("path")
    val path: String = ""

    @SerializedName("file_name")
    val fileName: String = ""

    @SerializedName("type_file")
    val typeFile: String = ""

    @SerializedName("url")
    val url: Url = Url()
}