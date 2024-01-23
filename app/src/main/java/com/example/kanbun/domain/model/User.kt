package com.example.kanbun.domain.model

import com.example.kanbun.common.AuthProvider

data class User(
    val id: String,
    val email: String,
    val name: String?,
    val profilePicture: String?,
    val authProvider: AuthProvider,
    val workspaces: List<WorkspaceInfo>,
    val cards: List<String>
) {
    data class WorkspaceInfo(
        val id: String = "",
        val name: String = ""
    )
}

