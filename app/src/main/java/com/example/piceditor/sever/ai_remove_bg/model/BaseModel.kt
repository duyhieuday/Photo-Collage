package com.example.piceditor.sever.ai_remove_bg.model

import com.google.gson.annotations.SerializedName
import java.util.Date

open class BaseModel {
    @SerializedName("id")
    val id: Long = 0
    @SerializedName("updated_at")
    val createdAt: Date = Date()
    @SerializedName("created_at")
    val updatedAt: Date = Date()
}