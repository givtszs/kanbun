package com.example.kanbun.domain.model

import android.os.Parcelable
import com.example.kanbun.common.Role
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Workspace(
    val id: String = "",
    val name: String = "",
    val owner: String = "",
    val members: Map<String, Role.Workspace> = emptyMap(),
    val boards: List<BoardInfo> = emptyList()
) : Parcelable {

//    @Parcelize
//    data class WorkspaceMember(
//        val id: String,
//        val role: @RawValue Role.Workspace
//    ) : Parcelable

    @Parcelize
    data class BoardInfo(
        val boardId: String,
        val workspaceId: String,
        val name: String,
        val cover: String?
    ) : Parcelable
}