package com.example.kanbun.data

import com.example.kanbun.common.AuthProvider
import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.WorkspaceInfo

object TestData {
    val user = User(
        id = "test1",
        email = "test@gmail.com",
        name = "Test",
        tag = "user_test1",
        profilePicture = null,
        authProvider = AuthProvider.GOOGLE,
        workspaces = listOf(WorkspaceInfo("work1", "Test Workspace 1")),
        sharedWorkspaces = emptyList(),
        sharedBoards = emptyMap(),
        cards = listOf("card1", "card2")
    )

    val firestoreUser = FirestoreUser(
        email = "test@gmail.com",
        name = "Test",
        profilePicture = null,
        authProvider = AuthProvider.GOOGLE.providerId,
        workspaces = mapOf("id" to "workspaces/work1"),
        cards = listOf("workspaces/work1/boards/board1/columns/col1/card1")
    )
}
