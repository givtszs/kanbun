package com.example.kanbun.common

import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.data.model.FirestoreWorkspace
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.UserWorkspace
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.model.WorkspaceMember
import com.google.firebase.auth.FirebaseUser

fun User.toFirestoreUser(): FirestoreUser =
    FirestoreUser(
        email = email,
        name = name,
        profilePicture = profilePicture,
        authProvider = authProvider.providerId,
        workspaces = workspaces.mapToFirestoreWorkspaces(),
        cards = cards
    )

fun List<UserWorkspace>.mapToFirestoreWorkspaces(): List<Map<String, String>> =
    map {
        mapOf("id" to it.id, "name" to it.name)
    }

fun FirestoreUser.toUser(userId: String): User =
    User(
        uid = userId,
        email = email,
        name = name,
        profilePicture = profilePicture,
        authProvider = AuthProvider.entries.first { it.providerId == authProvider },
        workspaces = workspaces.map {
            UserWorkspace(
                id = it["id"] ?: throw IllegalArgumentException("User `id` can't be null!"),
                name = it["name"]
                    ?: throw IllegalArgumentException("User `name` can't be null!")
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

fun Workspace.toFirestoreWorkspace(): FirestoreWorkspace =
    FirestoreWorkspace(
        name = name,
        owner = owner,
        members = members.map { member ->
            mapOf(
                "id" to FirestoreCollection.getReference(FirestoreCollection.USERS, member.id),
                "role" to member.role.roleName
            )
        },
        boards = boards
    )

fun FirestoreWorkspace.toWorkspace(workspaceId: String): Workspace =
    Workspace(
        uid = workspaceId,
        name = name,
        owner = owner,
        members = members.map { member ->
            WorkspaceMember(
                id = member["id"] ?: throw IllegalArgumentException("Workspace `id` can't be null!"),
                role = WorkspaceRole.entries.first {
                    it.roleName == (member["role"] ?: throw IllegalArgumentException("Workspace `role` can't be null!"))
                }
            )
        },
        boards = boards
    )