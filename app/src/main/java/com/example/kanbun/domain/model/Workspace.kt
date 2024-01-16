package com.example.kanbun.domain.model

data class Workspace(
    val uid: String,
    val name: String,
    val owner: String,
    val members: List<UserInfo>
)