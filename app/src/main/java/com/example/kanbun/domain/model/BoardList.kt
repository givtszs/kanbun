package com.example.kanbun.domain.model

data class BoardList(
    val id: String = "",
    val name: String = "",
    val position: Int = 0,
    val tasks: List<Task> = emptyList()
)
