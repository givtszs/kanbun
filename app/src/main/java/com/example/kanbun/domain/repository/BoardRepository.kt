package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

interface BoardRepository {

    /**
     * Adds a new [Board] entry in the Firestore database.
     *
     * @param board the board to add.
     * @return [Result] of [Unit] on success, or an error on failure.
     */
    suspend fun createBoard(board: Board): Result<Unit>

    /**
     * Retrieves the [Board] from the Firestore database for the given [boardId].
     *
     * @param boardId the id of the board to get.
     * @param workspaceId the id of the [Workspace] holding the board to get.
     * @return [Result] of [Board] on success, or an error on failure.
     */
    suspend fun getBoard(boardId: String, workspaceId: String): Result<Board>

    /**
     * Retrieves the stream of [Board] data from the Firestore database for the given [boardId].
     *
     * @param boardId the id of the board to get the data stream of.
     * @param workspaceId the id of the [Workspace] holding the board to get.
     * @return [Flow] of [Result] of [Board] data.
     */
    fun getBoardStream(boardId: String, workspaceId: String): Flow<Result<Board>>

    /**
     * Retrieves the list of boards for the given map of [sharedBoards] from the Firestore database.
     *
     * @param sharedBoards the map of [Board.id] to [Workspace.id] values.
     * @return [Result] of the list of [Board]s on success, or an error on failure.
     */
    suspend fun getSharedBoards(sharedBoards: Map<String, String>): Result<List<Board>>

    suspend fun updateBoard(oldBoard: Board, newBoard: Board): Result<Unit>

    /**
     * Deletes the given [board] from the Firestore database.
     */
    suspend fun deleteBoard(board: Board): Result<Unit>

    suspend fun upsertTag(tag: Tag, boardId: String, boardPath: String): Result<Tag>
}