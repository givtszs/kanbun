package com.example.kanbun.domain.model

import com.example.kanbun.common.WorkspaceRole

data class Workspace(
    val id: String = "",
    val name: String = "",
    val owner: String = "",
    val members: List<WorkspaceMember> = emptyList(), // userId to workspace role
    val boards: List<String> = emptyList()
)

data class WorkspaceMember(
    val id: String,
    val role: WorkspaceRole
)