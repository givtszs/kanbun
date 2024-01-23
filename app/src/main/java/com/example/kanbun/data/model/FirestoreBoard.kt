package com.example.kanbun.data.model

data class FirestoreBoard(
    val description: String = "",
    val owner: String = "",
    val settings: Map<String, Any?> = emptyMap(),
    val lists: List<String> = emptyList()
)