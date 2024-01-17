package com.example.kanbun.data.model

data class FirestoreWorkspace(
    val name: String = "",
    val owner: String = "",
    val members: List<Map<String, String>> = emptyList(), // user reference to user's workspace role
    val boards: List<String> = emptyList() // list of boards references
)