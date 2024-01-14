package com.example.kanbun.data

import com.example.kanbun.common.AuthProvider
import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.UserWorkspace

object TestData {
    val user = User(
        uid = "test1",
        email = "test@gmail.com",
        name = "Test",
        profilePicture = null,
        authProvider = AuthProvider.GOOGLE,
        workspaces = listOf(UserWorkspace("workspaces/work1", "Test Workspace 1")),
        cards = listOf("workspaces/work1/boards/board1/columns/col1/card1")
    )

    val firestoreUser = FirestoreUser(
        email = "test@gmail.com",
        name = "Test",
        profilePicture = null,
        authProvider = AuthProvider.GOOGLE.providerId,
        workspaces = listOf(mapOf("id" to "workspaces/work1", "name" to "Test Workspace 1")),
        cards = listOf("workspaces/work1/boards/board1/columns/col1/card1")
    )
}
