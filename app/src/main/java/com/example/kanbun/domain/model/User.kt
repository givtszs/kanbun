package com.example.kanbun.domain.model

data class User(
    val uid: String,
    val email: String,
    val name: String?,
    val profilePicture: String?,
    val authProviders: List<String>,
    val workspaces: List<UserWorkspace>,
    val cards: List<String>
)

data class UserWorkspace(
    val id: String,
    val name: String
)