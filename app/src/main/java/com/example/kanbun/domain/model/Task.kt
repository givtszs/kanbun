package com.example.kanbun.domain.model

import java.util.UUID

data class Task(
    val id: String = "",
    val position: Long = 0,
    val name: String = "",
    val description: String = "",
)
