package com.example.kanbun.domain.model

import com.example.kanbun.common.AuthType

data class User(
    val uid: String,
    val email: String,
    val name: String,
    val profilePicture: String,
    val authType: AuthType,
    val workspaces: List<UserWorkspace>,
    val cards: List<String>
)

data class UserWorkspace(
    val id: String,
    val name: String
)