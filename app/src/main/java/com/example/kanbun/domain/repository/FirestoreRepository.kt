package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining methods for Firestore interactions related to user data.
 */
interface FirestoreRepository {

    /**
     * Adds a user to Firestore.
     * @param user user instance to be added.
     * @return [Result] containing [Unit] on success, or an error message on failure.
     */
    suspend fun createUser(user: User): Result<Unit>

    /**
     * Retrieves a user from Firestore based on the [userId].
     * @param userId id of the user to retrieve.
     * @return [Result] containing the retrieved [User] on success, or an error message on failure.
     */
    suspend fun getUser(userId: String): Result<User>

    fun getUserStream(userId: String): Flow<User?>

    suspend fun createWorkspace(workspace: Workspace): Result<String>

    suspend fun getWorkspace(workspaceId: String): Result<Workspace>

    fun getWorkspaceStream(workspaceId: String): Flow<Workspace?>

    suspend fun updateWorkspaceName(workspace: Workspace, name: String): Result<Unit>

    suspend fun inviteToWorkspace(workspace: Workspace, user: User): Result<Unit>

    suspend fun deleteWorkspace(workspace: Workspace): Result<Unit>

    suspend fun createBoard(board: Board): Result<String>

    suspend fun getBoard(boardId: String, workspaceId: String): Result<Board>

    suspend fun createBoardList(boardList: BoardList, board: Board): Result<String>

    suspend fun getBoardList(boardListPath: String, boardListId: String): Result<BoardList>

    fun getBoardListsStream(boardId: String, workspaceId: String): Flow<Result<List<BoardList>>>

    suspend fun createTask(
        task: Task,
        listId: String,
        listPath: String
    ): Result<String>

    suspend fun updateTask(task: Task, boardListPath: String, boardListId: String): Result<Unit>

    suspend fun rearrangeTasksPositions(
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

    suspend fun rearrangeBoardListsPositions(
        listsPath: String,
        boardLists: List<BoardList>,
        from: Int,
        to: Int
    ): Result<Unit>

    suspend fun createTag(
        tag: Tag,
        boardId: String,
        boardPath: String
    ): Result<String>

    suspend fun getTags(boardId: String, workspaceId: String): Result<List<Tag>>

    suspend fun getTaskTags(
        boardId: String,
        workspaceId: String,
        tagIds: List<String>
    ): Result<List<Tag>>
}