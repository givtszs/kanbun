package com.example.kanbun.data.repository

import android.util.Log
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.TAG
import com.example.kanbun.common.getResult
import com.example.kanbun.common.runCatching
import com.example.kanbun.common.toBoard
import com.example.kanbun.common.toFirestoreBoard
import com.example.kanbun.common.toFirestoreBoardInfo
import com.example.kanbun.common.toFirestoreBoardMembers
import com.example.kanbun.common.toFirestoreTag
import com.example.kanbun.common.toFirestoreTags
import com.example.kanbun.data.model.FirestoreBoard
import com.example.kanbun.di.IoDispatcher
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.repository.BoardRepository
import com.example.kanbun.domain.repository.FirestoreRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class BoardRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val firestoreRepository: FirestoreRepository
) : BoardRepository {

    override suspend fun createBoard(board: Board): Result<Unit> = runCatching {
        withContext(dispatcher) {
            val workspaceId = board.workspace.id

            firestore.collection(FirestoreCollection.WORKSPACES).document(board.workspace.id)
                .collection(FirestoreCollection.BOARDS)
                .add(board.toFirestoreBoard())
                .getResult {
                    addBoardInfoToWorkspace(
                        workspaceId,
                        Workspace.BoardInfo(
                            boardId = result.id,
                            workspaceId = workspaceId,
                            name = board.name,
                            cover = board.cover
                        )
                    )
                }
        }
    }

    private fun addBoardInfoToWorkspace(
        workspaceId: String,
        boardInfo: Workspace.BoardInfo
    ) {
        firestore.collection(FirestoreCollection.WORKSPACES)
            .document(workspaceId)
            .update("boards.${boardInfo.boardId}", boardInfo.toFirestoreBoardInfo())
    }

    override suspend fun getBoard(boardId: String, workspaceId: String): Result<Board> =
        runCatching {
            withContext(dispatcher) {
                firestore
                    .collection(FirestoreCollection.WORKSPACES).document(workspaceId)
                    .collection(FirestoreCollection.BOARDS).document(boardId)
                    .get()
                    .getResult {
                        result.toObject(FirestoreBoard::class.java)?.toBoard(boardId)
                            ?: throw NullPointerException("Couldn't convert FirestoreBoard to Board since the value is null")
                    }
            }
        }

    override fun getBoardStream(boardId: String, workspaceId: String): Flow<Result<Board>> = callbackFlow {
        val listener = firestore.collection(FirestoreCollection.WORKSPACES).document(workspaceId)
            .collection(FirestoreCollection.BOARDS).document(boardId)
            .addSnapshotListener { documentSnapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message))
                    close(error)
                    return@addSnapshotListener
                }

                val board = documentSnapshot?.toObject(FirestoreBoard::class.java)?.toBoard(documentSnapshot.id)
                    ?: throw NullPointerException("Couldn't convert FirestoreBoard to Board since the value is null")
                Log.d(TAG, "getBoardStream: board $board")
                trySend(Result.Success(board))
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun getSharedBoards(sharedBoards: Map<String, String>): Result<List<Board>> = runCatching {
        val boards = mutableListOf<Board>()
        val fetchedBoards = withContext(dispatcher) {
            sharedBoards.map { entry ->
                async {
                    getBoard(boardId = entry.key, workspaceId = entry.value)
                }
            }
        }.awaitAll()
        fetchedBoards.forEach { boardResult ->
            if (boardResult is Result.Success) {
                boards.add(boardResult.data)
            }
        }
        boards
    }

    override suspend fun updateBoard(oldBoard: Board, newBoard: Board): Result<Unit> =
        runCatching {
            withContext(dispatcher) {
                val boardUpdates = getBoardUpdates(oldBoard, newBoard)
                firestore.collection(FirestoreCollection.WORKSPACES).document(newBoard.workspace.id)
                    .collection(FirestoreCollection.BOARDS).document(newBoard.id)
                    .update(boardUpdates)
                    .getResult {
                        if (newBoard.name.isNotEmpty()) {
                            updateBoardInfoInWorkspace(newBoard)
                        }

                        if ("members" in boardUpdates) {
                            val oldMembersIds = oldBoard.members.map { it.id }
                            val newMembersIds = newBoard.members.map { it.id }
                            if (oldMembersIds != newMembersIds) {
                                val membersToAdd = newMembersIds.filterNot { it in oldMembersIds }
                                membersToAdd.forEach { memberId ->
                                    addBoardToUser(
                                        userId = memberId,
                                        boardId = newBoard.id,
                                        workspaceId = newBoard.workspace.id
                                    )
                                }

                                val membersToDelete = oldMembersIds.filterNot { it in newMembersIds }
                                membersToDelete.forEach {
                                    deleteSharedBoardFromUser(it, newBoard.id)
                                }
                            }
                        }

                    }
            }
        }

    private fun getBoardUpdates(oldBoard: Board, newBoard: Board): Map<String, Any> {
        val mapOfUpdates = mutableMapOf<String, Any>()
        if (newBoard.name != oldBoard.name) {
            mapOfUpdates["name"] = newBoard.name
        }
        if (newBoard.description != oldBoard.description) {
            mapOfUpdates["description"] = newBoard.description
        }
        if (newBoard.tags != oldBoard.tags) {
            mapOfUpdates["tags"] = newBoard.tags.toFirestoreTags()
        }
        if (newBoard.members != oldBoard.members) {
            mapOfUpdates["members"] = newBoard.members.toFirestoreBoardMembers()
        }
        return mapOfUpdates
    }

    private fun updateBoardInfoInWorkspace(board: Board) {
        firestore
            .collection(FirestoreCollection.WORKSPACES)
            .document(board.workspace.id)
            .update(
                "boards.${board.id}",
                mapOf(
                    "cover" to board.cover,
                    "name" to board.name
                )
            )
    }

    private fun addBoardToUser(userId: String, boardId: String, workspaceId: String) {
        firestore.collection(FirestoreCollection.USERS)
            .document(userId)
            .update("sharedBoards.$boardId", workspaceId)
    }

    private fun deleteSharedBoardFromUser(userId: String, boardId: String) {
        firestore.collection(FirestoreCollection.USERS)
            .document(userId)
            .update("sharedBoards.$boardId", FieldValue.delete())
    }

    override suspend fun deleteBoard(
        board: Board
    ): Result<Unit> = runCatching {
        withContext(dispatcher) {
            val boardPath = "${FirestoreCollection.WORKSPACES}/${board.workspace.id}/${FirestoreCollection.BOARDS}/${board.id}"
            val taskListsRef = boardPath + "/${FirestoreCollection.TASK_LISTS}"

            // delete board
            firestoreRepository.recursiveDelete(boardPath)

            // delete board lists
            firestoreRepository.recursiveDelete(taskListsRef)

            // delete the board information from the workspace it belongs to
            deleteBoardInfoFromWorkspace(board.workspace.id, board.id)

            // delete the board from its members
            deleteBoardFromMembers(board.id, board.members, this@withContext)
        }
    }

    private fun deleteBoardInfoFromWorkspace(
        workspaceId: String,
        boardId: String
    ) {
        firestore.collection(FirestoreCollection.WORKSPACES)
            .document(workspaceId)
            .update("boards.${boardId}", FieldValue.delete())
    }

    private fun deleteBoardFromMembers(
        boardId: String,
        members: List<Board.BoardMember>,
        scope: CoroutineScope
    ) {
        members.forEach { member ->
            scope.launch {
                firestore.collection(FirestoreCollection.USERS)
                    .document(member.id)
                    .update("sharedBoards.$boardId", FieldValue.delete())
            }
        }
    }

    override suspend fun upsertTag(
        tag: Tag,
        boardId: String,
        boardPath: String,
    ): Result<Tag> = runCatching {
        withContext(dispatcher) {
            val tagId = tag.id.ifEmpty { UUID.randomUUID().toString() }
            firestore.collection(boardPath)
                .document(boardId)
                .update("tags.$tagId", tag.toFirestoreTag())
                .getResult {
                    tag.copy(id = tagId)
                }
        }
    }
}