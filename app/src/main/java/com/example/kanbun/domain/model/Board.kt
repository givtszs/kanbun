package com.example.kanbun.domain.model

import com.example.kanbun.common.BoardRole

data class Board(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val owner: String = "",
    val workspace: User.WorkspaceInfo = User.WorkspaceInfo(),
    val cover: String? = null,
    val members: List<BoardMember> = emptyList(),
    val lists: List<String> = emptyList(),
    val tags: List<Tag> = emptyList()
) {
    data class BoardMember(
        val id: String,
        val role: BoardRole
    )
}
