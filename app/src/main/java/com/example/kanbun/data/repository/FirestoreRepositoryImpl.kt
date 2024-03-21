package com.example.kanbun.data.repository

import android.util.Log
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.WorkspaceRole
import com.example.kanbun.common.runCatching
import com.example.kanbun.common.toBoard
import com.example.kanbun.common.toBoardList
import com.example.kanbun.common.toFirestoreBoard
import com.example.kanbun.common.toFirestoreBoardInfo
import com.example.kanbun.common.toFirestoreBoardList
import com.example.kanbun.common.toFirestoreTag
import com.example.kanbun.common.toFirestoreTask
import com.example.kanbun.common.toFirestoreUser
import com.example.kanbun.common.toFirestoreWorkspace
import com.example.kanbun.common.toUser
import com.example.kanbun.common.toWorkspace
import com.example.kanbun.data.model.FirestoreBoard
import com.example.kanbun.data.model.FirestoreBoardList
import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.data.model.FirestoreWorkspace
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
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

private const val TAG = "FirestoreRepository"

class FirestoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreRepository {

    private suspend inline fun <T, R> Task<T>.getResult(onSuccess: Task<T>.() -> R): R {
        await()
        if (!isSuccessful) {
            throw exception?.cause
                ?: IllegalStateException("Firestore operation failed with unknown error.")
        }
        return onSuccess()
    }

    override suspend fun createUser(user: User): Result<Unit> = runCatching {
        firestore.collection(FirestoreCollection.USERS.collectionName)
            .document(user.id)
            .set(user.toFirestoreUser())
            .await()
    }

    override suspend fun getUser(userId: String): Result<User> = runCatching {
        firestore.collection(FirestoreCollection.USERS.collectionName)
            .document(userId)
            .get()
            .getResult {
                val firestoreUser = result.toObject(FirestoreUser::class.java)
                    ?: throw NullPointerException("Couldn't convert FirestoreUser to User since the value is null")

                firestoreUser.toUser(userId)
            }
    }


    override fun getUserStream(userId: String): Flow<User?> = callbackFlow {
        var listener: ListenerRegistration? = null
        if (userId.isEmpty()) {
            trySend(null)
        } else {
            listener = firestore.collection(FirestoreCollection.USERS.collectionName)
                .document(userId)
                .addSnapshotListener { documentSnapshot, exception ->
                    if (exception != null) {
                        close(exception)
                        return@addSnapshotListener
                    }

                    documentSnapshot?.let {
                        val user = it.toObject(FirestoreUser::class.java)?.toUser(userId)
                            ?: throw NullPointerException("Couldn't convert FirestoreUser to User since the value is null")
                        Log.d(TAG, "user: firestoreUser: $user")

                        trySend(user)
                    }
                }
        }

        awaitClose {
            listener?.remove()
        }
    }

    private suspend fun addWorkspaceInfoToUser(
        userId: String,
        workspaceInfo: WorkspaceInfo
    ): Result<Unit> =
        runCatching {
            firestore.collection(FirestoreCollection.USERS.collectionName)
                .document(userId)
                .update("workspaces.${workspaceInfo.id}", workspaceInfo.name)
                .await()
        }

    private fun deleteUserWorkspace(userId: String, workspaceId: String): Result<Unit> =
        runCatching {
            firestore.collection(FirestoreCollection.USERS.collectionName)
                .document(userId)
                .update("workspaces.$workspaceId", FieldValue.delete())
//                .await()
        }

    override suspend fun createWorkspace(workspace: Workspace): Result<Unit> =
        runCatching {
            firestore.collection(FirestoreCollection.WORKSPACES.collectionName)
                .add(workspace.toFirestoreWorkspace())
                .getResult {
                    // add the newly create workspace into the user's workspaces
                    val res = addWorkspaceInfoToUser(
                        workspace.owner,
                        WorkspaceInfo(result.id, workspace.name)
                    )

                    if (res is Result.Error) {
                        throw res.e!!
                    }

                    result.id
                }
        }

    override suspend fun getWorkspace(workspaceId: String): Result<Workspace> = runCatching {
        firestore.collection(FirestoreCollection.WORKSPACES.collectionName)
            .document(workspaceId)
            .get()
            .getResult {
                val firestoreWorkspace = result.toObject(FirestoreWorkspace::class.java)
                    ?: throw NullPointerException("Couldn't convert FirestoreWorkspace to Workspace since the value is null")

                Log.d(TAG, "$firestoreWorkspace")

                firestoreWorkspace.toWorkspace(workspaceId)
            }
    }

    override fun getWorkspaceStream(workspaceId: String): Flow<Workspace?> = callbackFlow {
        var listener: ListenerRegistration? = null
        if (workspaceId.isEmpty()) {
            trySend(null)
        } else {
            listener = firestore.collection(FirestoreCollection.WORKSPACES.collectionName)
                .document(workspaceId)
                .addSnapshotListener { docSnapshot, error ->
                    if (error != null) {
                        Log.d(TAG, "getWorkspaceStream: error occurred: ${error.message}")
                        trySend(null)
                        close(error)
                        return@addSnapshotListener
                    }

                    val workspace = docSnapshot?.toObject(FirestoreWorkspace::class.java)
                        ?.toWorkspace(workspaceId)
                        ?: throw NullPointerException("Couldn't convert FirestoreWorkspace to Workspace since the value is null")
                    trySend(workspace)
                }
        }

        awaitClose {
            listener?.remove()
        }
    }

    override suspend fun updateWorkspaceName(workspace: Workspace, name: String): Result<Unit> =
        runCatching {
            firestore.collection(FirestoreCollection.WORKSPACES.collectionName)
                .document(workspace.id)
                .update("name", name)
                .getResult {
                    // update workspace name for each user associated with current workspace
                    workspace.members.forEach { member ->
                        firestore.collection(FirestoreCollection.USERS.collectionName)
                            .document(member.id)
                            .update("workspaces.${workspace.id}", name)
                            .await()
                    }
                }
        }

    private suspend fun addMemberToWorkspace(
        workspaceId: String,
        memberId: String,
        memberRole: WorkspaceRole
    ): Result<Unit> = runCatching {
        firestore.collection(FirestoreCollection.WORKSPACES.collectionName)
            .document(workspaceId)
            .update("members.$memberId", memberRole.roleName)
            .await()
    }

    override suspend fun inviteToWorkspace(
        workspace: Workspace,
        user: User
    ): Result<Unit> = runCatching {
        // update workspace members
        val workspaceUpdResult = addMemberToWorkspace(
            workspace.id,
            user.id,
            WorkspaceRole.MEMBER
        )

        if (workspaceUpdResult is Result.Error) {
            throw workspaceUpdResult.e!!
        }

        // update user's workspaces
        val userUpdateResult = addWorkspaceInfoToUser(
            user.id,
            WorkspaceInfo(workspace.id, workspace.name)
        )

        if (userUpdateResult is Result.Error) {
            throw userUpdateResult.e!!
        }
    }

    /**
     * Calls the Cloud Function to recursively delete a document or collection specified by the [path]
     *
     * @param path the path to the document/collection
     * @throws Exception on failure
     */
    private suspend fun recursiveDelete(path: String) {
        val deleteFn = Firebase.functions.getHttpsCallable("recursiveDelete")
        deleteFn.call(hashMapOf("path" to path))
            .addOnSuccessListener {
                Log.d(TAG, "deleteWorkspaceCloudFn: workspace deletion succeeded")
            }
            .addOnFailureListener {
                Log.d(TAG, "deleteWorkspaceCloudFn: workspace deletion failed")
                throw it
            }
            .await()
    }

    override suspend fun deleteWorkspace(workspace: Workspace): Result<Unit> = runCatching {
        val workspacePath = "${FirestoreCollection.WORKSPACES.collectionName}/${workspace.id}"
        val boardsPath =
            "${FirestoreCollection.WORKSPACES.collectionName}/${workspace.id}/${FirestoreCollection.BOARDS.collectionName}"
        // delete the workspace
        recursiveDelete(workspacePath)

        // delete the workspace boards
        recursiveDelete(boardsPath)

        // delete the workspace reference from its members
        workspace.members.forEach { member ->
            deleteUserWorkspace(member.id, workspace.id)
        }
    }

    private suspend fun addBoardInfoToWorkspace(
        workspaceId: String,
        boardInfo: Workspace.BoardInfo
    ): Result<Unit> = runCatching {
        firestore.collection(FirestoreCollection.WORKSPACES.collectionName)
            .document(workspaceId)
            .update("boards.${boardInfo.boardId}", boardInfo.toFirestoreBoardInfo())
            .await()
    }

    override suspend fun createBoard(board: Board): Result<Unit> = runCatching {
        val workspaceId = board.workspace.id
        firestore.collection("${FirestoreCollection.WORKSPACES.collectionName}/$workspaceId/${FirestoreCollection.BOARDS.collectionName}")
            .add(board.toFirestoreBoard())
            .getResult {
                val workspaceUpdResult = addBoardInfoToWorkspace(
                    workspaceId,
                    Workspace.BoardInfo(
                        boardId = result.id,
                        workspaceId = workspaceId,
                        name = board.name,
                        cover = board.cover
                    )
                )

                if (workspaceUpdResult is Result.Error) {
                    throw workspaceUpdResult.e!!
                }

                result.id
            }
    }

    override suspend fun getBoard(boardId: String, workspaceId: String): Result<Board> =
        runCatching {
            val workspacePath = "${FirestoreCollection.WORKSPACES.collectionName}/$workspaceId"
            firestore.collection("$workspacePath/${FirestoreCollection.BOARDS.collectionName}")
                .document(boardId)
                .get()
                .getResult {
                    result.toObject(FirestoreBoard::class.java)?.toBoard(boardId)
                        ?: throw NullPointerException("Couldn't convert FirestoreBoard to Board since the value is null")
                }
        }

    private suspend fun updateWorkspaceBoard(board: Board) {
        firestore
            .collection(FirestoreCollection.WORKSPACES.collectionName)
            .document(board.workspace.id)
            .update(
                "boards.${board.id}",
                mapOf(
                    "cover" to board.cover,
                    "name" to board.name
                )
            )
            .await()
    }

    private suspend fun deleteBoardFromWorkspace(
        workspaceId: String,
        boardId: String
    ) {
        firestore.collection(FirestoreCollection.WORKSPACES.collectionName)
            .document(workspaceId)
            .update("boards.${boardId}", FieldValue.delete())
            .await()
    }

    override suspend fun deleteBoard(
        board: Board
    ): Result<Unit> = runCatching {
        val boardRef = "${FirestoreCollection.WORKSPACES.collectionName}/${board.workspace.id}" +
                "/${FirestoreCollection.BOARDS.collectionName}/${board.id}"
        // delete board and its board lists
        recursiveDelete(boardRef)

        // delete the board reference from the workspace it belongs to
        deleteBoardFromWorkspace(board.workspace.id, board.id)
    }


    private suspend fun updateBoardsList(board: Board, boardListId: String): Result<Unit> =
        runCatching {
            val workspacePath =
                "${FirestoreCollection.WORKSPACES.collectionName}/${board.workspace.id}"
            firestore.collection("$workspacePath/${FirestoreCollection.BOARDS.collectionName}")
                .document(board.id)
                .update("lists", board.lists + boardListId)
                .await()
        }

    override suspend fun createBoardList(
        boardList: BoardList,
        board: Board
    ): Result<Unit> = runCatching {
        val workspacePath =
            "${FirestoreCollection.WORKSPACES.collectionName}/${board.workspace.id}"
        val boardPath = "${FirestoreCollection.BOARDS.collectionName}/${board.id}"
        firestore.collection("$workspacePath/$boardPath/${FirestoreCollection.BOARD_LIST.collectionName}")
            .add(boardList.toFirestoreBoardList())
            .getResult {
                val updateResult = updateBoardsList(board, result.id)
                if (updateResult is Result.Error) {
                    throw updateResult.e!!
                }
            }
    }

    override suspend fun getBoardList(
        boardListPath: String,
        boardListId: String
    ): Result<BoardList> = runCatching {
        firestore.collection(boardListPath)
            .document(boardListId)
            .get()
            .getResult {
                result.toObject(FirestoreBoardList::class.java)
                    ?.toBoardList(boardListId, boardListPath)
                    ?: throw NullPointerException("Couldn't convert FirestoreBoardList to BoardList since the value is null")
            }
    }

    override suspend fun updateBoardListName(
        newName: String,
        boardListPath: String,
        boardListId: String
    ): Result<Unit> = runCatching {
        firestore.collection(boardListPath)
            .document(boardListId)
            .update("name", newName)
            .await()
    }

    private fun rearrangeBoardLists(position: Int, path: String, boardLists: List<BoardList>) {
        val boardListCollectionRef = firestore.collection(path)
        Log.d(TAG, "rearrangeBoardLists is called")
        for (i in (position + 1)..<boardLists.size) {
            Log.d(TAG, "rearrangeBoardLists: boardList: ${boardLists[i]}")
            updateBoardListPosition(
                collectionReference = boardListCollectionRef,
                documentId = boardLists[i].id,
                newPosition = boardLists[i].position.dec()
            )
        }
    }

    override suspend fun deleteBoardListAndRearrange(
        id: String,
        path: String,
        boardLists: List<BoardList>,
        deleteAt: Int
    ): Result<Unit> = runCatching {
        firestore.collection(path)
            .document(id)
            .delete()
            .getResult {
                rearrangeBoardLists(deleteAt, path, boardLists)
                deleteListFromBoard(path, id)
            }
    }

    private fun deleteListFromBoard(boardListPath: String, boardListId: String) {
        val boardRef = boardListPath.substringBefore("/lists")
        val boardPath = boardRef.substringBeforeLast("/")
        val boardId = boardRef.substringAfterLast("/")
        firestore.collection(boardPath)
            .document(boardId)
            .update("lists", FieldValue.arrayRemove(boardListId))
//            .await()
    }

    override fun getBoardListsStream(
        boardId: String,
        workspaceId: String
    ): Flow<Result<List<BoardList>>> = callbackFlow {
        Log.d(TAG, "getBoardListsStream is called")
        var listener: ListenerRegistration? = null
        if (boardId.isEmpty() || workspaceId.isEmpty()) {
            trySend(Result.Loading)
        } else {
            val workspacePath =
                "${FirestoreCollection.WORKSPACES.collectionName}/$workspaceId"
            val boardPath = "${FirestoreCollection.BOARDS.collectionName}/$boardId"
            val path = "$workspacePath/$boardPath/${FirestoreCollection.BOARD_LIST.collectionName}"
            listener = firestore
                .collection(path)
                .addSnapshotListener { querySnapshot, error ->
                    trySend(Result.Loading)
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
        }

        awaitClose {
            listener?.remove()
        }
    }

    override suspend fun createTask(
        task: com.example.kanbun.domain.model.Task,
        listId: String,
        listPath: String
    ): Result<Unit> = runCatching {
        val taskId = UUID.randomUUID().toString()
        firestore
            .collection(listPath)
            .document(listId)
            .update("tasks.${taskId}", task.toFirestoreTask())
            .getResult { taskId }
    }

    override suspend fun updateTask(
        task: com.example.kanbun.domain.model.Task,
        boardListPath: String,
        boardListId: String
    ): Result<Unit> =
        runCatching {
            firestore.collection(boardListPath)
                .document(boardListId)
                .update("tasks.${task.id}", task.toFirestoreTask())
                .await()
        }

    override suspend fun deleteTask(
        task: com.example.kanbun.domain.model.Task,
        boardListPath: String,
        boardListId: String
    ): Result<Unit> =
        runCatching {
            firestore.collection(boardListPath)
                .document(boardListId)
                .update("tasks.${task.id}", FieldValue.delete())
                .await()
        }

    private fun rearrange(
        tasks: List<com.example.kanbun.domain.model.Task>,
        from: Int,
        to: Int
    ): Map<String, Long> {
        val updMap = mutableMapOf<String, Long>()
        if (from < to) {
            for (i in (from + 1)..to) {
                updMap["tasks.${tasks[i].id}.position"] = tasks[i].position.dec()
            }
        } else {
            for (i in to..<from) {
                updMap["tasks.${tasks[i].id}.position"] = tasks[i].position.inc()
            }
        }

        updMap["tasks.${tasks[from].id}.position"] = to.toLong()

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

    override suspend fun rearrangeTasks(
        listPath: String,
        listId: String,
        tasks: List<com.example.kanbun.domain.model.Task>,
        from: Int,
        to: Int
    ): Result<Unit> = runCatching {
        val updatesMap = rearrange(tasks, from, to)
        Log.d("ItemTaskViewHolder", "FirestoreRepository#rearrange: updates: $updatesMap")
        updateTasksPositions(listPath, listId, updatesMap)
    }

    private fun deleteAndRearrange(
        tasks: List<com.example.kanbun.domain.model.Task>,
        from: Int
    ): Map<String, Any> {
        val updMap = mutableMapOf<String, Any>()

        for (i in (from + 1)..<tasks.size) {
            updMap["tasks.${tasks[i].id}.position"] = tasks[i].position.dec()
        }

        updMap["tasks.${tasks[from].id}"] = FieldValue.delete()

        return updMap
    }

    override suspend fun deleteTaskAndRearrange(
        listPath: String,
        listId: String,
        tasks: List<com.example.kanbun.domain.model.Task>,
        from: Int
    ): Result<Unit> = runCatching {
        val updatesMap = deleteAndRearrange(tasks, from)
        Log.d("ItemTaskViewHolder", "FirestoreRepository#delete: updates: $updatesMap")
        updateTasksPositions(listPath, listId, updatesMap)
    }

    private fun insertAndRearrange(
        listToInsert: List<com.example.kanbun.domain.model.Task>,
        task: com.example.kanbun.domain.model.Task,
        to: Int
    ): Map<String, Any> {
        val updMap = mutableMapOf<String, Any>()

        for (i in to..<listToInsert.size) {
            updMap["tasks.${listToInsert[i].id}.position"] = listToInsert[i].position.inc()
        }

        val newTask = task.copy(position = to.toLong())

        updMap["tasks.${task.id}"] = newTask.toFirestoreTask()

        return updMap
    }

    override suspend fun insertTaskAndRearrange(
        listPath: String,
        listId: String,
        tasks: List<com.example.kanbun.domain.model.Task>,
        task: com.example.kanbun.domain.model.Task,
        to: Int
    ): Result<Unit> = runCatching {
        val updatesMap = insertAndRearrange(tasks, task, to)
        Log.d("ItemTaskViewHolder", "FirestoreRepository#insert: updates: $updatesMap")
        updateTasksPositions(listPath, listId, updatesMap)
    }

    private fun updateBoardListPosition(
        collectionReference: CollectionReference,
        documentId: String,
        newPosition: Long
    ): Task<Void> {
        Log.d(
            "ItemBoardListViewHolder", "FirestoreRepository#updateBoardListPosition: " +
                    "docId: $documentId, newPos: $newPosition"
        )
        return collectionReference.document(documentId)
            .update("position", newPosition)
    }

    override suspend fun rearrangeBoardLists(
        boardListPath: String,
        boardLists: List<BoardList>,
        from: Int,
        to: Int
    ): Result<Unit> = runCatching {
        // TODO: test speed of the function with and without `await()`
        val collectionRef = firestore.collection(boardListPath)

        if (from < to) {
            boardLists.forEach { list ->
                if (list.position in (from + 1)..to) {
                    updateBoardListPosition(collectionRef, list.id, list.position.dec())
                }
            }

        } else if (from > to) {
            boardLists.forEach { list ->
                if (list.position in to..<from) {
                    updateBoardListPosition(collectionRef, list.id, list.position.inc())
                }
            }
        }

        val listToMove = boardLists[from]
        updateBoardListPosition(collectionRef, listToMove.id, to.toLong()).await()
    }

    override suspend fun upsertTag(
        tag: Tag,
        boardId: String,
        boardPath: String,
    ): Result<Tag> =
        runCatching {
            val tagId = tag.id.ifEmpty { UUID.randomUUID().toString() }
            firestore.collection(boardPath)
                .document(boardId)
                .update("tags.$tagId", tag.toFirestoreTag())
                .getResult {
                    tag.copy(id = tagId)
                }
        }

    override suspend fun getAllTags(boardId: String, workspaceId: String): Result<List<Tag>> =
        runCatching {
            val boardRes = getBoard(boardId, workspaceId)
            if (boardRes is Result.Error) {
                throw boardRes.e!!
            }

            (boardRes as Result.Success).data.tags.sortedBy { it.name }
        }

    override suspend fun getTaskTags(
        taskId: String,
        tagIds: List<String>,
        boardListId: String,
        boardListPath: String,
    ): Result<List<Tag>> = runCatching {
        val boardId = boardListPath
            .substringAfter("${FirestoreCollection.BOARDS.collectionName}/")
            .substringBefore("/${FirestoreCollection.BOARD_LIST.collectionName}")
        val workspaceId = boardListPath
            .substringAfter("${FirestoreCollection.WORKSPACES.collectionName}/")
            .substringBefore("/${FirestoreCollection.BOARDS.collectionName}")

        val result = getAllTags(boardId, workspaceId)
        if (result is Result.Error) {
            throw result.e!!
        }

        val tags = (result as Result.Success).data
        val taskTags = tags.filter { it.id in tagIds }
        val taskTagIds = taskTags.map { it.id }
        Log.d(TAG, "getTaskTags: (old) tagsIds: $tagIds, (current) taskTagsIds: $taskTagIds")

        // if the passed tagIds and the received taskTagIds are different, it means that some tags
        // have been deleted from the board, so we need to update the task with only relevant tags.
        if (tagIds != taskTagIds) {
            Log.d(TAG, "getTaskTags: newTags: $taskTagIds")
            updateTaskTags(
                tagIds = taskTagIds,
                taskId = taskId,
                boardListId = boardListId,
                boardListPath = boardListPath
            )
        }

        taskTags
    }

    override suspend fun updateTaskTags(
        taskId: String,
        tagIds: List<String>,
        boardListId: String,
        boardListPath: String
    ): Result<Unit> = runCatching {
        firestore.collection(boardListPath)
            .document(boardListId)
            .update("tasks.$taskId.tags", tagIds)
            .await()
    }

    override suspend fun updateBoard(board: Board, updates: Map<String, Any>): Result<Unit> {
//        TODO("Not yet implemented")
        return Result.Error("Not yet implemented")
    }
}
