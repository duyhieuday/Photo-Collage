package com.example.piceditor.model

data class ToolItem(
    val icon: Int,
    val title: String,
    var isSelected: Boolean = false
)