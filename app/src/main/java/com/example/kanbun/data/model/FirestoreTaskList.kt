package com.example.kanbun.data.model

data class FirestoreTaskList(
    val name: String = "",
    val position: Long = 0,
    val path: String = "",
    val tasks: Map<String, FirestoreTask> = emptyMap()
)
