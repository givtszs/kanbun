package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Task

interface TaskRepository {

    /**
     * Adds a new [Task] entry in the Firestore database.
     *
     * @param task the task to add.
     * @param taskListId the id of the task list in which to add the task.
     * @param taskListPath the Firestore path of the task list collection.
     * @return [Result] of [Unit] on success, or an error on failure.
     */
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

    /**
     * Deletes the given [Task] from the Firestore database.
     *
     * @param task the task to delete.
     * @param taskListId the id of the task list in which to delete the task.
     * @param taskListPath the Firestore path of the task list collection.
     * @return [Result] of [Unit] on success, or an error on failure.
     */
    suspend fun deleteTask(task: Task, taskListPath: String, taskListId: String): Result<Unit>

    /**
     * Rearranges [tasks] in the Firestore database for the given [starting][from] and [ending][to] positions.
     *
     * @param tasks the list of tasks to rearrange.
     * @param taskListId the id of the task list in which to delete the task.
     * @param taskListPath the Firestore path of the task list collection.
     * @param from the starting position.
     * @param to the ending position.
     * @return [Result] of [Unit] on success, or an error on failure.
     */
    suspend fun rearrangeTasks(
        taskListPath: String,
        taskListId: String,
        tasks: List<Task>,
        from: Int,
        to: Int
    ): Result<Unit>

    /**
     * Removes the task from the task list at the given [position][from] and rearranges other tasks.
     * This method should be called when the task is being dragged from one task list to another.
     *
     * @param tasks the list of tasks to rearrange.
     * @param taskListId the id of the task list in which to delete the task.
     * @param taskListPath the Firestore path of the task list collection.
     * @param from the position to remove the task from.
     * @return [Result] of [Unit] on success, or an error on failure.
     */
    suspend fun removeTaskAndRearrange(
        taskListPath: String,
        taskListId: String,
        tasks: List<Task>,
        from: Int
    ): Result<Unit>

    /**
     * Inserts the [task] in the [task list][tasks] at the given [position][to].
     * This method should be called when the task is being dragged from one task list to another.
     *
     * @param task the task to insert.
     * @param tasks the list of tasks to insert the task into.
     * @param taskListId the id of the task list in which to delete the task.
     * @param taskListPath the Firestore path of the task list collection.
     * @return [Result] of [Unit] on success, or an error on failure.
     */
    suspend fun insertTaskAndRearrange(
        taskListPath: String,
        taskListId: String,
        tasks: List<Task>,
        task: Task,
        to: Int
    ): Result<Unit>
}