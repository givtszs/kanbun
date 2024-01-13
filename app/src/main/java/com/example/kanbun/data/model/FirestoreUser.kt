package com.example.kanbun.data.model

data class FirestoreUser(
    val email: String,
    val name: String?,
    val profilePicture: String?,
    val authProvider: String,
    val workspaces: List<Map<String, String>>,
    val cards: List<String>
)