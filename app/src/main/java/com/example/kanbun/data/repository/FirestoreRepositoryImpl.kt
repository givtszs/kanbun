package com.example.kanbun.data.repository

import android.util.Log
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.Role
import com.example.kanbun.common.TAG
import com.example.kanbun.common.WorkspaceType
import com.example.kanbun.common.getResult
import com.example.kanbun.common.runCatching
import com.example.kanbun.common.toBoard
import com.example.kanbun.common.toBoardList
import com.example.kanbun.common.toFirestoreBoard
import com.example.kanbun.common.toFirestoreBoardInfo
import com.example.kanbun.common.toFirestoreBoardList
import com.example.kanbun.common.toFirestoreBoardMembers
import com.example.kanbun.common.toFirestoreMembers
import com.example.kanbun.common.toFirestoreTag
import com.example.kanbun.common.toFirestoreTags
import com.example.kanbun.common.toFirestoreTask
import com.example.kanbun.common.toFirestoreWorkspace
import com.example.kanbun.common.toUser
import com.example.kanbun.common.toWorkspace
import com.example.kanbun.data.model.FirestoreBoard
import com.example.kanbun.data.model.FirestoreBoardList
import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.data.model.FirestoreWorkspace
import com.example.kanbun.di.IoDispatcher
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.model.WorkspaceInfo
import com.example.kanbun.domain.repository.FirestoreRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class FirestoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FirestoreRepository {

    /**
     * Calls the Cloud Function to recursively delete a document or collection specified by the [path]
     *
     * @param path the path to the document/collection
     * @throws Exception on failure
     */
    override fun recursiveDelete(path: String): Task<HttpsCallableResult> {
        val deleteFn = Firebase.functions.getHttpsCallable("recursiveDelete")
        return deleteFn.call(hashMapOf("path" to path))
            .addOnSuccessListener {
                Log.d(TAG, "Deletion succeeded")
            }
            .addOnFailureListener {
                Log.d(TAG, "Deletion failed")
                throw it
            }
    }

    override suspend fun createBoardList(
        boardList: BoardList,
        board: Board
    ): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            firestore.collection(FirestoreCollection.WORKSPACES).document(board.workspace.id)
                .collection(FirestoreCollection.BOARDS).document(board.id)
                .collection(FirestoreCollection.TASK_LISTS)
                .add(boardList.toFirestoreBoardList())
                .getResult {
                    updateBoardListsOfBoard(board, result.id)
                }
        }
    }

    private fun updateBoardListsOfBoard(board: Board, boardListId: String) {
        firestore.collection(FirestoreCollection.WORKSPACES).document(board.workspace.id)
            .collection(FirestoreCollection.BOARDS)
            .document(board.id)
            .update("lists", board.lists + boardListId)
    }

    override suspend fun getBoardList(
        boardListPath: String,
        boardListId: String
    ): Result<BoardList> = runCatching {
        withContext(ioDispatcher) {
            firestore.collection(boardListPath)
                .document(boardListId)
                .get()
                .getResult {
                    result.toObject(FirestoreBoardList::class.java)
                        ?.toBoardList(boardListId, boardListPath)
                        ?: throw NullPointerException("Couldn't convert FirestoreBoardList to BoardList since the value is null")
                }
        }
    }

    override fun getBoardListsStream(
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

    override suspend fun updateBoardListName(
        newName: String,
        boardListPath: String,
        boardListId: String
    ): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            firestore.collection(boardListPath)
                .document(boardListId)
                .update("name", newName)
        }
    }

    override suspend fun deleteBoardListAndRearrange(
        id: String,
        path: String,
        boardLists: List<BoardList>,
        deleteAt: Int
    ): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            firestore.collection(path)
                .document(id)
                .delete()
                .getResult {
                    if (boardLists.size != 1) {
                        rearrangeBoardLists(
                            boardListPath = path,
                            boardLists = boardLists,
                            from = deleteAt,
                            to = boardLists.size - 1
                        )
                    }
                    deleteListFromBoard(path, id)
                }
        }
    }

    private fun deleteListFromBoard(boardListPath: String, boardListId: String) {
        val boardRef = boardListPath.substringBefore("/lists")
        val boardPath = boardRef.substringBeforeLast("/")
        val boardId = boardRef.substringAfterLast("/")
        firestore.collection(boardPath)
            .document(boardId)
            .update("lists", FieldValue.arrayRemove(boardListId))
    }

    override suspend fun rearrangeBoardLists(
        boardListPath: String,
        boardLists: List<BoardList>,
        from: Int,
        to: Int
    ): Result<Unit> = runCatching {
        val collectionRef = firestore.collection(boardListPath)

        if (from < to) {
            for (i in (from + 1)..to) {
                val list = boardLists[i]
                updateBoardListPosition(collectionRef, list.id, list.position.dec())
            }

        } else if (from > to) {
            for (i in to..<from) {
                val list = boardLists[i]
                updateBoardListPosition(collectionRef, list.id, list.position.inc())
            }
        }

        val listToMove = boardLists[from]
        updateBoardListPosition(collectionRef, listToMove.id, to.toLong()) //.await()
    }

    private fun updateBoardListPosition(
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

    override suspend fun createTask(
        task: com.example.kanbun.domain.model.Task,
        listId: String,
        listPath: String
    ): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val taskId = UUID.randomUUID().toString()
            firestore.collection(listPath)
                .document(listId)
                .update("${FirestoreCollection.TASKS}.${taskId}", task.toFirestoreTask())
                .await()
        }
    }

    override suspend fun updateTask(
        oldTask: com.example.kanbun.domain.model.Task,
        newTask: com.example.kanbun.domain.model.Task,
        boardListId: String,
        boardListPath: String
    ): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val taskUpdates = getTaskUpdates(oldTask, newTask)
            Log.d(TAG, "updateTask: taskUpdates: $taskUpdates")
            firestore.collection(boardListPath)
                .document(boardListId)
                .update(taskUpdates)
        }
    }

    private fun getTaskUpdates(
        oldTask: com.example.kanbun.domain.model.Task,
        newTask: com.example.kanbun.domain.model.Task
    ): Map<String, Any?> {
        val mapOfUpdates = mutableMapOf<String, Any?>()
        val taskId = newTask.id
        if (newTask.name != oldTask.name) {
            mapOfUpdates["${FirestoreCollection.TASKS}.$taskId.name"] = newTask.name
        }
        if (newTask.description != oldTask.description) {
            mapOfUpdates["${FirestoreCollection.TASKS}.$taskId.description"] = newTask.description
        }
        if (newTask.dateStarts != oldTask.dateStarts) {
            mapOfUpdates["${FirestoreCollection.TASKS}.$taskId.dateStarts"] = newTask.dateStarts
        }
        if (newTask.dateEnds != oldTask.dateEnds) {
            mapOfUpdates["${FirestoreCollection.TASKS}.$taskId.dateEnds"] = newTask.dateEnds
        }
        if (newTask.tags != oldTask.tags) {
            mapOfUpdates["${FirestoreCollection.TASKS}.$taskId.tags"] = newTask.tags
        }
        if (newTask.members != oldTask.members) {
            mapOfUpdates["${FirestoreCollection.TASKS}.$taskId.members"] = newTask.members
        }
        return mapOfUpdates
    }

    override suspend fun deleteTask(
        task: com.example.kanbun.domain.model.Task,
        boardListPath: String,
        boardListId: String
    ): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            firestore.collection(boardListPath)
                .document(boardListId)
                .update("${FirestoreCollection.TASKS}.${task.id}", FieldValue.delete())
        }
    }

    override suspend fun rearrangeTasks(
        listPath: String,
        listId: String,
        tasks: List<com.example.kanbun.domain.model.Task>,
        from: Int,
        to: Int
    ): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val updatesMap = getRearrangeUpdates(tasks, from, to)
            Log.d("ItemTaskViewHolder", "FirestoreRepository#rearrange: updates: $updatesMap")
            updateTasksPositions(listPath, listId, updatesMap)
        }
    }

    private fun getRearrangeUpdates(
        tasks: List<com.example.kanbun.domain.model.Task>,
        from: Int,
        to: Int
    ): Map<String, Long> {
        val updMap = mutableMapOf<String, Long>()
        if (from < to) {
            for (i in (from + 1)..to) {
                updMap["${FirestoreCollection.TASKS}.${tasks[i].id}.position"] =
                    tasks[i].position.dec()
            }
        } else {
            for (i in to..<from) {
                updMap["${FirestoreCollection.TASKS}.${tasks[i].id}.position"] =
                    tasks[i].position.inc()
            }
        }
        updMap["${FirestoreCollection.TASKS}.${tasks[from].id}.position"] = to.toLong()
        return updMap
    }

    override suspend fun deleteTaskAndRearrange(
        listPath: String,
        listId: String,
        tasks: List<com.example.kanbun.domain.model.Task>,
        from: Int
    ): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val updatesMap = deleteAndRearrange(tasks, from)
            Log.d("ItemTaskViewHolder", "FirestoreRepository#delete: updates: $updatesMap")
            updateTasksPositions(listPath, listId, updatesMap)
        }
    }

    private fun deleteAndRearrange(
        tasks: List<com.example.kanbun.domain.model.Task>,
        from: Int
    ): Map<String, Any> {
        val updMap = mutableMapOf<String, Any>()
        for (i in (from + 1)..<tasks.size) {
            updMap["${FirestoreCollection.TASKS}.${tasks[i].id}.position"] = tasks[i].position.dec()
        }
        updMap["${FirestoreCollection.TASKS}.${tasks[from].id}"] = FieldValue.delete()
        return updMap
    }

    override suspend fun insertTaskAndRearrange(
        listPath: String,
        listId: String,
        tasks: List<com.example.kanbun.domain.model.Task>,
        task: com.example.kanbun.domain.model.Task,
        to: Int
    ): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val updatesMap = insertAndRearrange(tasks, task, to)
            Log.d("ItemTaskViewHolder", "FirestoreRepository#insert: updates: $updatesMap")
            updateTasksPositions(listPath, listId, updatesMap)
        }
    }

    private fun insertAndRearrange(
        listToInsert: List<com.example.kanbun.domain.model.Task>,
        task: com.example.kanbun.domain.model.Task,
        to: Int
    ): Map<String, Any> {
        val updMap = mutableMapOf<String, Any>()
        for (i in to..<listToInsert.size) {
            updMap["${FirestoreCollection.TASKS}.${listToInsert[i].id}.position"] =
                listToInsert[i].position.inc()
        }
        val newTask = task.copy(position = to.toLong())
        updMap["${FirestoreCollection.TASKS}.${task.id}"] = newTask.toFirestoreTask()
        return updMap
    }

    private suspend fun updateTasksPositions(
        listPath: String,
        listId: String,
        updatesMap: Map<String, Any>
    ) {
        firestore.collection(listPath)
            .document(listId)
            .update(updatesMap)
            .await()
    }

    override suspend fun upsertTag(
        tag: Tag,
        boardId: String,
        boardPath: String,
    ): Result<Tag> = runCatching {
        withContext(ioDispatcher) {
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