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

    suspend fun updateUser(userId: String, updates: Map<String, String?>): Result<Unit>

    /**
     * Checks if the user tag is already taken.
     *
     * @param tag the user tag
     * @return `true` if the tag is already taken, `false` otherwise
     */
    suspend fun isTagTaken(tag: String): Result<Boolean>

    suspend fun findUsersByTag(tag: String): Result<List<User>>

    suspend fun createWorkspace(workspace: Workspace): Result<Unit>

    suspend fun getWorkspace(workspaceId: String): Result<Workspace>

    fun getWorkspaceStream(workspaceId: String): Flow<Workspace?>

    suspend fun updateWorkspace(oldWorkspace: Workspace, newWorkspace: Workspace): Result<Unit>

    /**
     * Deletes the [workspace] using the deployed Cloud Function which recursively deletes all
     * data in the document, including sub collections.
     *
     * @param workspace the workspace to delete
     * @return the result of the function execution
     */
    suspend fun deleteWorkspace(workspace: Workspace): Result<Unit>

    suspend fun createBoard(board: Board): Result<Unit>

    suspend fun getBoard(boardId: String, workspaceId: String): Result<Board>

    fun getBoardStream(boardId: String, workspaceId: String): Flow<Result<Board>>

    suspend fun updateBoard(oldBoard: Board, newBoard: Board): Result<Unit>

    suspend fun deleteBoard(board: Board): Result<Unit>

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

    /**
     * Gets all tags for the specified board.
     *
     * @param boardId the board id
     * @param workspaceId the workspace id that hosts the board
     * @return the [Result] containing a list of fetched board tags
     */
    suspend fun getAllTags(boardId: String, workspaceId: String): Result<List<Tag>>

    suspend fun getTaskTags(
        task: Task,
        boardListId: String,
        boardListPath: String,
    ): Result<List<Tag>>

    suspend fun getMembers(userIds: List<String>): Result<List<User>>

    suspend fun getSharedBoards(sharedBoards: Map<String, String>): Result<List<Board>>
}