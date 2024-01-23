package com.example.kanbun.data

import com.example.kanbun.common.AuthProvider
import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.domain.model.User

object TestData {
    val user = User(
        id = "test1",
        email = "test@gmail.com",
        name = "Test",
        profilePicture = null,
        authProvider = AuthProvider.GOOGLE,
        workspaces = listOf(User.WorkspaceInfo("workspaces/work1", "Test Workspace 1")),
        cards = listOf("workspaces/work1/boards/board1/columns/col1/card1")
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
