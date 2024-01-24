package com.example.kanbun.data.model

data class FirestoreBoardList(
    val name: String = "",
    val position: Int = 0,
    val cards: Map<String, Map<String, Any?>> = emptyMap()
)
