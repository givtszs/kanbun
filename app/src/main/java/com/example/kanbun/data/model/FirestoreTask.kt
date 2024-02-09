package com.example.kanbun.data.model

import com.example.kanbun.domain.model.BoardListInfo

data class FirestoreTask(
    val position: Long = 0,
    val name: String = "",
    val description: String = "",
    val author: String = "",
    val tags: List<String> = emptyList(),
    val members: List<String> = emptyList(),
    val dateStarts: String = "",
    val dateEnds: String = ""
)