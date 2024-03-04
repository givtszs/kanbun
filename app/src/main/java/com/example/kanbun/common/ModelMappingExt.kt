package com.example.kanbun.common

import com.example.kanbun.data.model.FirestoreBoard
import com.example.kanbun.data.model.FirestoreBoardList
import com.example.kanbun.data.model.FirestoreTag
import com.example.kanbun.data.model.FirestoreTask
import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.data.model.FirestoreWorkspace
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.model.WorkspaceInfo
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

fun List<WorkspaceInfo>.toFirestoreWorkspaces(): Map<String, String> =
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
            WorkspaceInfo(
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
        name = name,
        description = description,
        owner = owner,
        workspace = mapOf(
            "id" to workspace.id,
            "name" to workspace.name
        ),
        cover = cover,
        members = members.toFirestoreBoardMembers(),
        lists = lists,
        tags = tags.toFirestoreTags()
    )

fun List<Tag>.toFirestoreTags(): Map<String, FirestoreTag> =
    associate { tag ->
        tag.id to tag.toFirestoreTag()
    }

fun List<Board.BoardMember>.toFirestoreBoardMembers() =
    associate { member ->
        member.id to member.role.roleName
    }

fun FirestoreBoard.toBoard(boardId: String): Board =
    Board(
        id = boardId,
        name = name,
        description = description,
        owner = owner,
        workspace = WorkspaceInfo(
            id = workspace["id"] as String,
            name = workspace["name"] as String
        ),
        cover = cover,
        members = members.map { entry ->
            Board.BoardMember(
                id = entry.key,
                role = BoardRole.entries.first { it.roleName == entry.value })
        },
        lists = lists,
        tags = tags.toTags()
    )

fun Map<String, FirestoreTag>.toTags(): List<Tag> =
    map { entry ->
        entry.value.toTag(entry.key)
    }

fun BoardList.toFirestoreBoardList(): FirestoreBoardList =
    FirestoreBoardList(
        name = name,
        position = position,
        path = path,
        tasks = tasks.associate {
            it.id to it.toFirestoreTask()
        }
    )

fun FirestoreBoardList.toBoardList(boardListId: String, boardListPath: String): BoardList =
    BoardList(
        id = boardListId,
        name = name,
        position = position,
        path = boardListPath,
        tasks = tasks
            .map { entry ->
                entry.value.toTask(entry.key)
            }
            .sortedBy { task -> task.position }
    )

fun Task.toFirestoreTask(): FirestoreTask =
    FirestoreTask(
        name = name,
        position = position,
        description = description,
        author = author,
        tags = tags,
        members = members,
        dateStarts = dateStarts,
        dateEnds = dateEnds
    )

fun FirestoreTask.toTask(id: String): Task =
    Task(
        id = id,
        name = name,
        position = position,
        description = description,
        author = author,
        tags = tags,
        members = members,
        dateStarts = dateStarts,
        dateEnds = dateEnds
    )

fun Tag.toFirestoreTag(): FirestoreTag =
    FirestoreTag(
        name = name,
        color = color
    )

fun FirestoreTag.toTag(tagId: String): Tag =
    Tag(
        id = tagId,
        name = name,
        color = color
    )