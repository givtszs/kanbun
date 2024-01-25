package com.example.kanbun.domain.model

import android.os.Parcelable
import com.example.kanbun.common.WorkspaceRole
import kotlinx.parcelize.Parcelize

@Parcelize
data class Workspace(
    val id: String = "",
    val name: String = "",
    val owner: String = "",
    val members: List<WorkspaceMember> = emptyList(),
    val boards: List<BoardInfo> = emptyList()
) : Parcelable {

    @Parcelize
    data class WorkspaceMember(
        val id: String,
        val role: WorkspaceRole
    ) : Parcelable

    @Parcelize
    data class BoardInfo(
        val boardId: String,
        val workspaceId: String,
        val name: String,
        val cover: String?
    ) : Parcelable
}