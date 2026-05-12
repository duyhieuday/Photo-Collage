package com.example.piceditor.sever.ai_remove_bg.model

import com.google.gson.annotations.SerializedName

data class CategoryModel(
    @SerializedName("label")
    val label: String = "",
    @SerializedName("models")
    val models: List<GenArtModel>? = emptyList(),
//    val videoModels: List<AIVideoFragment.VideoUiItem>? = null
) : BaseModel()