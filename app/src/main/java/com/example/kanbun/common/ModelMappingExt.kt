package com.example.kanbun.common

import com.example.kanbun.data.model.FirestoreBoard
import com.example.kanbun.data.model.FirestoreBoardList
import com.example.kanbun.data.model.FirestoreTask
import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.data.model.FirestoreWorkspace
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
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

fun List<User.WorkspaceInfo>.toFirestoreWorkspaces(): Map<String, String> =
    associate { workspace ->
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
            User.WorkspaceInfo(
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
        profilePicture = data.photoUrl?.toString(),
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
        boards = boards.toFirestoreBoards()
    )

fun Workspace.BoardInfo.toFirestoreBoardInfo(): Map<String, String?> =
    mapOf(
        "name" to name,
        "cover" to cover
    )

fun List<Workspace.WorkspaceMember>.toFirestoreMembers(): Map<String, String> =
    associate { member ->
        member.id to member.role.roleName
    }

fun List<Workspace.BoardInfo>.toFirestoreBoards(): Map<String, Map<String, String?>> =
    associate { boardInfo ->
        boardInfo.boardId to mapOf(
            "name" to boardInfo.name,
            "cover" to boardInfo.cover
        )
    }

fun FirestoreWorkspace.toWorkspace(workspaceId: String): Workspace =
    Workspace(
        id = workspaceId,
        name = name,
        owner = owner,
        members = members.map { entry ->
            Workspace.WorkspaceMember(
                id = entry.key,
                role = WorkspaceRole.entries.first { it.roleName == entry.value }
            )
        },
        boards = boards.map { entry ->
            val values = entry.value
            Workspace.BoardInfo(
                boardId = entry.key,
                workspaceId = workspaceId,
                name = values["name"]!!,
                cover = values["cover"]
            )
        }
    )

fun Board.toFirestoreBoard(): FirestoreBoard =
    FirestoreBoard(
        description = description,
        owner = owner,
        settings = settings.toFirestoreBoardSettings(),
        lists = lists
    )

fun Board.BoardSettings.toFirestoreBoardSettings(): Map<String, Any?> =
    mapOf(
        "name" to name,
        "workspace" to mapOf("id" to workspace.id, "name" to workspace.name),
        "cover" to cover,
        "members" to members.associate { member ->
            member.id to member.role.roleName
        }
    )

fun FirestoreBoard.toBoard(boardId: String): Board =
    Board(
        id = boardId,
        description = description,
        owner = owner,
        settings = Board.BoardSettings(
            name = settings["name"] as String,
            workspace = (settings["workspace"] as Map<String, String>).run {
                User.WorkspaceInfo(id = this["id"]!!, name = this["name"]!!)
            },
            cover = settings["cover"] as String?,
            members = (settings["members"] as Map<String, String>).map { entry ->
                Board.BoardMember(
                    id = entry.key,
                    role = BoardRole.entries.first { it.roleName == entry.value })
            }
        ),
        lists = lists
    )

fun BoardList.toFirestoreBoardList(): FirestoreBoardList =
    FirestoreBoardList(
        name = name,
        position = position,
        tasks = tasks.associate { task ->
            task.id to mapOf(
                "position" to task.position,
                "name" to task.name,
                "description" to task.description
            )
        }
    )

fun FirestoreBoardList.toBoardList(boardListId: String): BoardList =
    BoardList(
        id = boardListId,
        name = name,
        position = position,
        tasks = tasks.map { entry ->
            val values = entry.value
            Task(
                id = entry.key,
                position = values["position"] as Long,
                name = values["name"] as String,
                description = values["description"] as String
            )
        }.sortedBy { task ->
            task.position
        }
    )

fun Task.toMap(): Map<String, Any> =
    mapOf(
        "position" to position,
        "name" to name,
        "description" to description
    )