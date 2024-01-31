package com.example.kanbun.domain.model

data class BoardList(
    val id: String = "",
    val name: String = "",
    val position: Long = 0,
    val path: String = "",
    val tasks: List<Task> = emptyList()
)
