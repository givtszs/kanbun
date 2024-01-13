package com.example.kanbun.data

import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.UserWorkspace
import com.google.firebase.auth.FirebaseUser
import com.example.kanbun.common.AuthProvider

fun User.toFirestoreUser(): FirestoreUser =
    FirestoreUser(
        email = email,
        name = name,
        profilePicture = profilePicture,
        authProvider = authProvider.providerId,
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
        authProvider = AuthProvider.entries.first { it.providerId == authProvider },
        workspaces = workspaces.map {
            UserWorkspace(
                id = it["id"] ?: throw IllegalArgumentException("Workspace `id` can't be null!"),
                name = it["name"] ?: throw IllegalArgumentException("Workspace `name` can't be null!")
            )
        },
        cards = cards
    )

fun FirebaseUser.toUser(provider: AuthProvider): User {
    val data = providerData.first { it.providerId == provider.providerId }
    return User(
        uid = uid,
        email = data.email!!,
        name = data.displayName,
        profilePicture = data.photoUrl.toString(),
        authProvider = provider,
        workspaces = emptyList(),
        cards = emptyList()
    )
}