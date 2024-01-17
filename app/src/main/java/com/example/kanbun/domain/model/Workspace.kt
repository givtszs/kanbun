package com.example.kanbun.domain.model

import com.example.kanbun.common.WorkspaceRole

data class Workspace(
    val uid: String = "",
    val name: String,
    val owner: String,
    val members: List<WorkspaceMember>, // userId to workspace role
    val boards: List<String>
)

data class WorkspaceMember(
    val id: String,
    val role: WorkspaceRole
)