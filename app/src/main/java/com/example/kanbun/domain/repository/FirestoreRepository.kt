package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining methods for Firestore interactions related to user data.
 */
interface FirestoreRepository {

    fun recursiveDelete(path: String): com.google.android.gms.tasks.Task<HttpsCallableResult>

    suspend fun createBoardList(boardList: BoardList, board: Board): Result<Unit>

    suspend fun getBoardList(boardListPath: String, boardListId: String): Result<BoardList>

    fun getBoardListsStream(boardId: String, workspaceId: String): Flow<Result<List<BoardList>>>

    suspend fun updateBoardListName(
        newName: String,
        boardListPath: String,
        boardListId: String
    ): Result<Unit>

    suspend fun deleteBoardListAndRearrange(
        id: String,
        path: String,
        boardLists: List<BoardList>,
        deleteAt: Int
    ): Result<Unit>

    suspend fun rearrangeBoardLists(
        boardListPath: String,
        boardLists: List<BoardList>,
        from: Int,
        to: Int
    ): Result<Unit>

    suspend fun createTask(
        task: Task,
        listId: String,
        listPath: String
    ): Result<Unit>

    suspend fun updateTask(
        oldTask: Task,
        newTask: Task,
        boardListId: String,
        boardListPath: String,
    ): Result<Unit>

    suspend fun deleteTask(task: Task, boardListPath: String, boardListId: String): Result<Unit>

    suspend fun rearrangeTasks(
        listPath: String,
        listId: String,
        tasks: List<Task>,
        from: Int,
        to: Int
    ): Result<Unit>

    suspend fun deleteTaskAndRearrange(
        listPath: String,
        listId: String,
        tasks: List<Task>,
        from: Int
    ): Result<Unit>

    suspend fun insertTaskAndRearrange(
        listPath: String,
        listId: String,
        tasks: List<Task>,
        task: Task,
        to: Int
    ): Result<Unit>

    suspend fun upsertTag(
        tag: Tag,
        boardId: String,
        boardPath: String
    ): Result<Tag>
}