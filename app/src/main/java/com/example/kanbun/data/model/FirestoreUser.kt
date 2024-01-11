package com.example.kanbun.data.model

data class FirestoreUser(
    val email: String = "",
    val name: String? = "",
    val profilePicture: String? = "",
    val authProviders: List<String> = emptyList(),
    val workspaces: List<Map<String, String>> = emptyList(),
    val cards: List<String> = emptyList()
)