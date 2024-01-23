package com.example.kanbun.data.model

data class FirestoreWorkspace(
    val name: String = "",
    val owner: String = "",
    val members: Map<String, String> = emptyMap(), // user reference to user's workspace role
    val boards: Map<String, Map<String, String?>> = emptyMap() // list of boards references
)