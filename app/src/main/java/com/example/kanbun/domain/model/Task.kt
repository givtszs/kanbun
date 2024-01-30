package com.example.kanbun.domain.model

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class Task(
    val id: String = "",
    val position: Long = 0,
    val name: String = "",
    val description: String = "",
)
