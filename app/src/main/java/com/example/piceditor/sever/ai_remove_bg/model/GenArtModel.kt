package com.example.piceditor.sever.ai_remove_bg.model

import com.google.gson.annotations.SerializedName


class GenArtModel : BaseModel() {
    @SerializedName("label")
    val label: String? = ""
    @SerializedName("images")
    val images: List<Media>? = emptyList()
    @SerializedName("prompt")
    val prompt: String? = ""
}