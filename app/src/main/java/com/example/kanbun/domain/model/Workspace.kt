package com.example.kanbun.domain.model

import android.os.Parcelable
import com.example.kanbun.common.WorkspaceRole
import kotlinx.parcelize.Parcelize

@Parcelize
data class Workspace(
    val id: String = "",
    val name: String = "",
    val owner: String = "",
    val members: List<WorkspaceMember> = emptyList(), // userId to workspace role
    val boards: List<BoardInfo> = emptyList()
) : Parcelable {

    @Parcelize
    data class WorkspaceMember(
        val id: String,
        val role: WorkspaceRole
    ) : Parcelable

    @Parcelize
    data class BoardInfo(
        val id: String,
        val name: String,
        val cover: String?
    ) : Parcelable
}