package com.example.kanbun.data

import com.example.kanbun.common.getAuthType
import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.UserWorkspace

fun User.toFirestoreUser(): FirestoreUser =
    FirestoreUser(
        email = email,
        name = name,
        profilePicture = profilePicture,
        authType = authType.typeName,
        workspaces = workspaces.map {
            mapOf("id" to it.id, "name" to it.name)
        },
        cards = cards
    )

fun FirestoreUser.toUser(userId: String): User =
    User(
        uid = userId,
        email = email,
        name = name,
        profilePicture = profilePicture,
        authType = getAuthType(authType) ?: throw IllegalArgumentException("AuthType can't be null!"),
        workspaces = workspaces.map {
            UserWorkspace(
                id = it["id"] ?: throw IllegalArgumentException("Workspace `id` can't be null!"),
                name = it["name"] ?: throw IllegalArgumentException("Workspace `name` can't be null!")
            )
        },
        cards = cards
    )