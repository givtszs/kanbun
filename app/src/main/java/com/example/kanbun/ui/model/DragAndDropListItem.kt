package com.example.kanbun.ui.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DragAndDropListItem(
    val initPosition: Int
)