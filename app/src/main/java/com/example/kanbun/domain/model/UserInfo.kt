package com.example.kanbun.domain.model

data class UserInfo(
    val uid: String,
    val name: String,
    val email: String,
    val profilePicture: String?,
    val role: String
)