package com.example.kanbun.data.model

data class FirestoreBoard(
    val name: String = "",
    val description: String = "",
    val owner: String = "",
    val workspace: Map<String, String> = emptyMap(),
    val cover: String? = null,
    val lists: List<String> = emptyList(),
    val members: Map<String, String> = emptyMap(),
    val tags: Map<String, FirestoreTag> = emptyMap()
)