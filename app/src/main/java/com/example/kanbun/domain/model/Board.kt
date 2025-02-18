package com.example.kanbun.domain.model

import android.os.Parcelable
import com.example.kanbun.common.Role
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Board(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val owner: String = "",
    val workspace: WorkspaceInfo = WorkspaceInfo(),
    val cover: String? = null,
    val members: List<BoardMember> = emptyList(),
    val lists: List<String> = emptyList(),
    val tags: List<Tag> = emptyList()
) : Parcelable {

    @Parcelize
    data class BoardMember(
        val id: String,
        val role: @RawValue Role.Board
    ) : Parcelable
}
