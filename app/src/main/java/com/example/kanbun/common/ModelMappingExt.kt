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
        workspaces = workspaces.toFirestoreWorkspaces(),
        cards = cards
    )

fun List<UserWorkspace>.toFirestoreWorkspaces(): Map<String, String> = associate { workspace ->
    workspace.id to workspace.name
}

fun FirestoreUser.toUser(userId: String): User =
    User(
        id = userId,
        email = email,
        name = name,
        profilePicture = profilePicture,
        authProvider = AuthProvider.entries.first { it.providerId == authProvider },
        workspaces = workspaces.map { entry ->
            UserWorkspace(
                id = entry.key,
                name = entry.value
            )
        },
        cards = cards
    )

fun FirebaseUser.toUser(provider: AuthProvider): User {
    val data = providerData.first { it.providerId == provider.providerId }
    return User(
        id = uid,
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
        members = members.toFirestoreMembers(),
        boards = boards
    )

fun List<WorkspaceMember>.toFirestoreMembers(): Map<String, String> = associate { member ->
    member.id to member.role.roleName
}

fun FirestoreWorkspace.toWorkspace(workspaceId: String): Workspace =
    Workspace(
        id = workspaceId,
        name = name,
        owner = owner,
        members = members.map { entry ->
            WorkspaceMember(
                id = entry.key,
                role = WorkspaceRole.entries.first { it.roleName == entry.value }
            )
        },
        boards = boards
    )