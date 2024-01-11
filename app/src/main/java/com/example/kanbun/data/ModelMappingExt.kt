package com.example.kanbun.data

import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.UserWorkspace

fun User.toFirestoreUser(): FirestoreUser =
    FirestoreUser(
        email = email,
        name = name,
        profilePicture = profilePicture,
        authProviders = authProviders,
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
        authProviders = authProviders,
        workspaces = workspaces.map {
            UserWorkspace(
                id = it["id"] ?: throw IllegalArgumentException("Workspace `id` can't be null!"),
                name = it["name"] ?: throw IllegalArgumentException("Workspace `name` can't be null!")
            )
        },
        cards = cards
    )