package com.example.kanbun.data.model

data class FirestoreUser(
    val email: String = "",
    val name: String? = null,
    val profilePicture: String? = null,
    val authProvider: String = "",
    val workspaces: Map<String, String> = emptyMap(),
    val cards: List<String> = emptyList()
)