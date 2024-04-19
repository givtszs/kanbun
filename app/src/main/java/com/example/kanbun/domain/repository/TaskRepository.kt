package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Task

interface TaskRepository {
    suspend fun createTask(
        task: Task,
        taskListId: String,
        taskListPath: String
    ): Result<Unit>

    suspend fun updateTask(
        oldTask: Task,
        newTask: Task,
        taskListId: String,
        taskListPath: String,
    ): Result<Unit>

    suspend fun deleteTask(task: Task, taskListPath: String, taskListId: String): Result<Unit>

    suspend fun rearrangeTasks(
        taskListPath: String,
        taskListId: String,
        tasks: List<Task>,
        from: Int,
        to: Int
    ): Result<Unit>

    suspend fun deleteTaskAndRearrange(
        taskListPath: String,
        taskListId: String,
        tasks: List<Task>,
        from: Int
    ): Result<Unit>

    suspend fun insertTaskAndRearrange(
        taskListPath: String,
        taskListId: String,
        tasks: List<Task>,
        task: Task,
        to: Int
    ): Result<Unit>
}