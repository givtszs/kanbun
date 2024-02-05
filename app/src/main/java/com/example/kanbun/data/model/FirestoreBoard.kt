package com.example.kanbun.data.model

import com.example.kanbun.domain.model.Tag

data class FirestoreBoard(
    val description: String = "",
    val owner: String = "",
    val settings: Map<String, Any?> = emptyMap(),
    val lists: List<String> = emptyList(),
    val tags: List<Tag> = emptyList()
)