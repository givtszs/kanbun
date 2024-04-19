package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.google.firebase.functions.HttpsCallableResult

/**
 * Interface defining methods for Firestore interactions related to user data.
 */
interface FirestoreRepository {

    fun recursiveDelete(path: String): com.google.android.gms.tasks.Task<HttpsCallableResult>

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