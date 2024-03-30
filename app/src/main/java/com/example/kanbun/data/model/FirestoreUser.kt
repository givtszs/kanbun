package com.example.kanbun.data.model

data class FirestoreUser(
    val tag: String = "",
    val email: String = "",
    val name: String? = null,
    val profilePicture: String? = null,
    val authProvider: String = "",
    val workspaces: Map<String, String> = emptyMap(),
    val sharedWorkspaces: Map<String, String> = emptyMap(),
    val sharedBoards: List<String> = emptyList(),
    val cards: List<String> = emptyList()
)