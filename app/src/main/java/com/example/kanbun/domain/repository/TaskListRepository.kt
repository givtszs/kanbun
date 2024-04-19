package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.TaskList
import kotlinx.coroutines.flow.Flow

interface TaskListRepository {

    /**
     * Adds a new [TaskList] entry in the Firestore database for the given [Board].
     *
     * @param taskList the task list to add.
     * @param board the board to save the task list into.
     * @return [Result] of [Unit] on success, or an error on failure.
     */
    suspend fun createTaskList(taskList: TaskList, board: Board): Result<Unit>

    /**
     * Retrieves the [TaskList] from the Firestore database for the given [taskListId].
     *
     * @param taskListId the id of the task list to get.
     * @param taskListPath the Firestore path of the task list document to get.
     * @return [Result] of with the retrieved [TaskList] on success, or an error on failure.
     */
    suspend fun getTaskList(taskListPath: String, taskListId: String): Result<TaskList>

    /**
     * Retrieves the stream of [TaskList]s data from the Firestore database for the given [Board].
     *
     * @param boardId the id of the board for which to retrieve the data stream of task lists.
     * @param workspaceId the id of the workspace holding the board.
     * @return [Flow] of the task lists on success, or an error on failure.
     */
    fun getTaskListStream(boardId: String, workspaceId: String): Flow<Result<List<TaskList>>>

    /**
     * Updates the name of the [TaskList] with the provided new [name].
     *
     * @param name the new name to set to the task list.
     * @param taskListId the id of the task list to update the name for.
     * @param taskListPath the Firestore path of the task list document.
     * @return [Result] of [Unit] on success, or an error on failure.
     */
    suspend fun updateTaskListName(
        name: String,
        taskListPath: String,
        taskListId: String
    ): Result<Unit>

    /**
     * Deletes the task list at the [given position][deleteAt] from the Firestore database and
     * updates the positions of other [taskLists].
     *
     * @param id the id of the task list to delete.
     * @param path the Firestore path of the task list document.
     * @param taskLists the task lists to update the positions for after the deletion.
     * @param deleteAt the position of the task list to delete.
     * @return [Result] of [Unit] on success, or an error on failure.
     */
    suspend fun deleteTaskListAndRearrange(
        id: String,
        path: String,
        taskLists: List<TaskList>,
        deleteAt: Int
    ): Result<Unit>

    /**
     * Rearranges the given [taskLists] from the [starting][from] position to the [ending][to].
     *
     * @param taskListPath the Firestore path to the task list collection.
     * @param taskLists the list of [TaskList]s to rearrange positions for.
     * @param from the starting position.
     * @param to the ending position.
     * @return [Result] of [Unit] on success, or an error on failure.
     */
    suspend fun rearrangeTaskLists(
        taskListPath: String,
        taskLists: List<TaskList>,
        from: Int,
        to: Int
    ): Result<Unit>
}