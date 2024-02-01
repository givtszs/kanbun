package com.example.kanbun.ui.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BoardListInfo(
    val id: String,
    val path: String
)