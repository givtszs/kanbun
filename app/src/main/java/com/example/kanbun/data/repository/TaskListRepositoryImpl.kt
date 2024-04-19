package com.example.kanbun.data.repository

import android.util.Log
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.TAG
import com.example.kanbun.common.getResult
import com.example.kanbun.common.runCatching
import com.example.kanbun.common.toBoardList
import com.example.kanbun.common.toFirestoreBoardList
import com.example.kanbun.data.model.FirestoreBoardList
import com.example.kanbun.di.IoDispatcher
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.repository.TaskListRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TaskListRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : TaskListRepository {

    override suspend fun createTaskList(
        taskList: BoardList,
        board: Board
    ): Result<Unit> = runCatching {
        withContext(dispatcher) {
            firestore.collection(FirestoreCollection.WORKSPACES).document(board.workspace.id)
                .collection(FirestoreCollection.BOARDS).document(board.id)
                .collection(FirestoreCollection.TASK_LISTS)
                .add(taskList.toFirestoreBoardList())
                .getResult {
                    addTaskListToBoard(board, result.id)
                }
        }
    }

    private fun addTaskListToBoard(board: Board, boardListId: String) {
        firestore.collection(FirestoreCollection.WORKSPACES).document(board.workspace.id)
            .collection(FirestoreCollection.BOARDS)
            .document(board.id)
            .update("lists", board.lists + boardListId)
    }

    override suspend fun getTaskList(
        taskListPath: String,
        taskListId: String
    ): Result<BoardList> = runCatching {
        withContext(dispatcher) {
            firestore.collection(taskListPath)
                .document(taskListId)
                .get()
                .getResult {
                    result.toObject(FirestoreBoardList::class.java)
                        ?.toBoardList(taskListId, taskListPath)
                        ?: throw NullPointerException("Couldn't convert FirestoreBoardList to BoardList since the value is null")
                }
        }
    }

    override fun getTaskListStream(
        boardId: String,
        workspaceId: String
    ): Flow<Result<List<BoardList>>> = callbackFlow {
        Log.d(TAG, "getBoardListsStream is called")
        val workspacePath =
            "${FirestoreCollection.WORKSPACES}/$workspaceId"
        val boardPath = "${FirestoreCollection.BOARDS}/$boardId"
        val path = "$workspacePath/$boardPath/${FirestoreCollection.TASK_LISTS}"
        val listener = firestore.collection(FirestoreCollection.WORKSPACES).document(workspaceId)
            .collection(FirestoreCollection.BOARDS).document(boardId)
            .collection(FirestoreCollection.TASK_LISTS)
            .addSnapshotListener { querySnapshot, error ->
                querySnapshot?.let {
                    val boardLists = it.documents.map { docSnapshot ->
                        val boardList = docSnapshot.toObject(FirestoreBoardList::class.java)
                            ?.toBoardList(docSnapshot.id, path)
                            ?: throw NullPointerException("Couldn't convert FirestoreBoardList to BoardList since the value is null")
                        boardList
                    }.reversed()
                    Log.d(TAG, "getBoardListsFlow#boardLists: $boardLists")
                    trySend(Result.Success(boardLists))
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun updateTaskListName(
        name: String,
        taskListPath: String,
        taskListId: String
    ): Result<Unit> = runCatching {
        withContext(dispatcher) {
            firestore.collection(taskListPath)
                .document(taskListId)
                .update("name", name)
        }
    }

    override suspend fun deleteTaskListAndRearrange(
        id: String,
        path: String,
        taskLists: List<BoardList>,
        deleteAt: Int
    ): Result<Unit> = runCatching {
        withContext(dispatcher) {
            firestore.collection(path)
                .document(id)
                .delete()
                .getResult {
                    if (taskLists.size != 1) {
                        rearrangeTaskLists(
                            taskListPath = path,
                            taskLists = taskLists,
                            from = deleteAt,
                            to = taskLists.size - 1
                        )
                    }
                    deleteTaskListFromBoard(path, id)
                }
        }
    }

    private fun deleteTaskListFromBoard(boardListPath: String, boardListId: String) {
        val boardRef = boardListPath.substringBefore("/lists")
        val boardPath = boardRef.substringBeforeLast("/")
        val boardId = boardRef.substringAfterLast("/")
        firestore.collection(boardPath)
            .document(boardId)
            .update("lists", FieldValue.arrayRemove(boardListId))
    }

    override suspend fun rearrangeTaskLists(
        taskListPath: String,
        taskLists: List<BoardList>,
        from: Int,
        to: Int
    ): Result<Unit> = runCatching {
        val collectionRef = firestore.collection(taskListPath)

        if (from < to) {
            for (i in (from + 1)..to) {
                val list = taskLists[i]
                updateTaskListPosition(collectionRef, list.id, list.position.dec())
            }

        } else if (from > to) {
            for (i in to..<from) {
                val list = taskLists[i]
                updateTaskListPosition(collectionRef, list.id, list.position.inc())
            }
        }

        val listToMove = taskLists[from]
        updateTaskListPosition(collectionRef, listToMove.id, to.toLong()) //.await()
    }

    private fun updateTaskListPosition(
        collectionReference: CollectionReference,
        documentId: String,
        newPosition: Long
    ) {
        Log.d(
            "ItemBoardListViewHolder", "FirestoreRepository#updateBoardListPosition: " +
                    "docId: $documentId, newPos: $newPosition"
        )
        collectionReference.document(documentId)
            .update("position", newPosition)
    }
}