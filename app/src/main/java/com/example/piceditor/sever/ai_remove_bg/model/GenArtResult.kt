package com.example.piceditor.sever.ai_remove_bg.model

import com.google.gson.annotations.SerializedName


class GenArtResult : BaseModel() {
    @SerializedName("type")
    val type: Int = 0

    @SerializedName("status")
    val status: Int = 0

    @SerializedName("value")
    val value: OutputData = OutputData()

    fun isDone(): Boolean {
        return status == 2 || status == 3
    }
}