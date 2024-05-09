package com.example.kanbun.domain.model

import com.example.kanbun.common.AuthProvider

data class User(
    val id: String,
    val tag: String,
    val email: String,
    val name: String?,
    val profilePicture: String?,
    val authProvider: AuthProvider,
    val workspaces: List<WorkspaceInfo>,
    val sharedWorkspaces: List<WorkspaceInfo>,
    val sharedBoards: Map<String, String>,
    val tasks: Map<String, String>
)