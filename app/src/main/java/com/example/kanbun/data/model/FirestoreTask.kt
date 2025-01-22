package com.example.kanbun.data.model

data class FirestoreTask(
    val position: Long = 0,
    val name: String = "",
    val description: String = "",
    val author: String = "",
    val tags: List<String> = emptyList(),
    val members: List<String> = emptyList(),
    val dateStarts: Long? = null,
    val dateEnds: Long? = null
)