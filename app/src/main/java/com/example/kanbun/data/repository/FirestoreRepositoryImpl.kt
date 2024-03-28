package com.example.kanbun.data.repository

import android.util.Log
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.WorkspaceRole
import com.example.kanbun.common.WorkspaceType
import com.example.kanbun.common.runCatching
import com.example.kanbun.common.toBoard
import com.example.kanbun.common.toBoardList
import com.example.kanbun.common.toFirestoreBoard
import com.example.kanbun.common.toFirestoreBoardInfo
import com.example.kanbun.common.toFirestoreBoardList
import com.example.kanbun.common.toFirestoreMembers
import com.example.kanbun.common.toFirestoreTag
import com.example.kanbun.common.toFirestoreTags
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
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
    private val ioDispatcher: CoroutineDispatcher
) : FirestoreRepository {
    private val TAG = "RefactoredFirestoreRepository"

    private suspend inline fun <T, R> Task<T>.getResult(onSuccess: Task<T>.() -> R): R {
        await()
        if (!isSuccessful) {
            throw exception?.cause
                ?: IllegalStateException("Firestore operation failed with an unknown error.")
        }
        return onSuccess()
    }

    override suspend fun createUser(user: User): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            firestore.collection(FirestoreCollection.USERS)
                .document(user.id)
                .set(user.toFirestoreUser())
        }
    }

    override suspend fun getUser(userId: String): Result<User> = runCatching {
        withContext(ioDispatcher) {
            firestore.collection(FirestoreCollection.USERS)
                .document(userId)
                .get()
                .addOnSuccessListener { }
                .getResult {
                    val firestoreUser = result.toObject(FirestoreUser::class.java)
                        ?: throw NullPointerException("Couldn't convert FirestoreUser to User since the value is null")
                    Log.d(TAG, "getUser: firestoreUser: $firestoreUser")
                    firestoreUser.toUser(userId)
                }
        }
    }

    override fun getUserStream(userId: String): Flow<User?> = callbackFlow {
        var listener: ListenerRegistration? = null
        if (userId.isEmpty()) {
            trySend(null)
        } else {
            listener = firestore.collection(FirestoreCollection.USERS)
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

    override suspend fun findUsersByTag(tag: String): Result<List<User>> = runCatching {
        withContext(ioDispatcher) {
            firestore.collection(FirestoreCollection.USERS)
                .whereGreaterThanOrEqualTo("tag", tag)
                .whereLessThanOrEqualTo("tag", tag + '\uf8ff')
                .get()
                .getResult {
                    val users = this.result.documents.map { document ->
                        document.toObject(FirestoreUser::class.java)?.toUser(document.id)
                            ?: throw NullPointerException("Couldn't convert FirestoreUser to User since the value is null")
                    }
                    Log.d(TAG, "findUsersByTag: $users")
                    users
                }
        }
    }

    override suspend fun createWorkspace(workspace: Workspace): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            firestore.collection(FirestoreCollection.WORKSPACES)
                .add(workspace.toFirestoreWorkspace())
                .getResult {
                    addWorkspaceInfoToUser(
                        userId = workspace.owner,
                        workspaceInfo = WorkspaceInfo(result.id, workspace.name)
                    )
                }
        }
    }

    /**
     * Adds the [workspace information][WorkspaceInfo] to the user's list of workspaces.
     *
     * @param userId the user id
     * @param workspaceInfo the object containing the workspace information
     */
    private fun addWorkspaceInfoToUser(
        userId: String,
        workspaceInfo: WorkspaceInfo,
        isSharedWorkspace: Boolean = false
    ) {
        val fieldName =
            if (!isSharedWorkspace) WorkspaceType.USER_WORKSPACE else WorkspaceType.SHARED_WORKSPACE
        firestore.collection(FirestoreCollection.USERS)
            .document(userId)
            .update("$fieldName.${workspaceInfo.id}", workspaceInfo.name)
    }

    private fun deleteWorkspaceInfoFromUser(
        userId: String,
        workspaceId: String
    ) {
        firestore.collection(FirestoreCollection.USERS)
            .document(userId)
            .update("${WorkspaceType.SHARED_WORKSPACE}.$workspaceId", FieldValue.delete())
    }

    override suspend fun getWorkspace(workspaceId: String): Result<Workspace> = runCatching {
        withContext(ioDispatcher) {
            firestore.collection(FirestoreCollection.WORKSPACES)
                .document(workspaceId)
                .get()
                .getResult {
                    val firestoreWorkspace = result.toObject(FirestoreWorkspace::class.java)
                        ?: throw NullPointerException("Couldn't convert FirestoreWorkspace to Workspace since the value is null")

                    Log.d(TAG, "$firestoreWorkspace")

                    firestoreWorkspace.toWorkspace(workspaceId)
                }
        }
    }

    override fun getWorkspaceStream(workspaceId: String): Flow<Workspace?> = callbackFlow {
        var listener: ListenerRegistration? = null
        if (workspaceId.isEmpty()) {
            trySend(null)
        } else {
            listener = firestore.collection(FirestoreCollection.WORKSPACES)
                .document(workspaceId)
                .addSnapshotListener { docSnapshot, error ->
                    if (error != null) {
                        Log.d(TAG, "getWorkspaceStream: error occurred: ${error.message}")
//                        trySend(null)
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

    override suspend fun updateWorkspace(
        oldWorkspace: Workspace,
        newWorkspace: Workspace
    ): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val workspaceUpdates = getWorkspaceUpdates(oldWorkspace, newWorkspace)
            firestore.collection(FirestoreCollection.WORKSPACES)
                .document(newWorkspace.id)
                .update(workspaceUpdates)
                .getResult {
                    if ("name" in workspaceUpdates) {
                        updateWorkspaceNameInUserWorkspaces(
                            scope = this@withContext,
                            workspaceId = newWorkspace.id,
                            members = newWorkspace.members,
                            name = workspaceUpdates["name"] as String
                        )
                    }

                    if ("members" in workspaceUpdates) {
                        val oldMembersIds = oldWorkspace.members.map { it.id }
                        val newMembersIds = newWorkspace.members.map { it.id }
                        if (oldMembersIds != newMembersIds) {
                            val membersToAdd = newMembersIds.filterNot { it in oldMembersIds }
                            membersToAdd.forEach { memberId ->
                                addWorkspaceInfoToUser(
                                    userId = memberId,
                                    workspaceInfo = WorkspaceInfo(
                                        id = newWorkspace.id,
                                        name = newWorkspace.name
                                    ),
                                    isSharedWorkspace = true
                                )
                            }

                            val membersToDelete = oldMembersIds.filterNot { it in newMembersIds }
                            membersToDelete.forEach {
                                deleteWorkspaceInfoFromUser(it, newWorkspace.id)
                            }
                        }
                    }
                }
        }
    }

    private fun getWorkspaceUpdates(
        oldWorkspace: Workspace,
        newWorkspace: Workspace
    ): Map<String, Any> {
        val mapOfUpdates = mutableMapOf<String, Any>()
        if (newWorkspace.name != oldWorkspace.name) {
            mapOfUpdates["name"] = newWorkspace.name
        }
        if (newWorkspace.members != oldWorkspace.members) {
            mapOfUpdates["members"] = newWorkspace.members.toFirestoreMembers()
        }
        return mapOfUpdates
    }

    private fun updateWorkspaceNameInUserWorkspaces(
        workspaceId: String,
        members: List<Workspace.WorkspaceMember>,
        name: String,
        scope: CoroutineScope
    ) {
        members.map { user ->
            scope.launch {
                firestore.collection(FirestoreCollection.USERS)
                    .document(user.id)
                    .update("workspaces.$workspaceId", name)
            }
        }
    }

    private fun addMemberToWorkspace(
        workspaceId: String,
        memberId: String,
    ): Result<Unit> = runCatching {
        firestore.collection(FirestoreCollection.WORKSPACES)
            .document(workspaceId)
            .update("members.$memberId", WorkspaceRole.MEMBER.roleName)
    }

    override suspend fun inviteToWorkspace(
        workspace: Workspace,
        user: User
    ): Result<Unit> = runCatching {
        // update workspace members
        addMemberToWorkspace(
            workspace.id,
            user.id
        )

        // update user's workspaces
        addWorkspaceInfoToUser(
            userId = user.id,
            workspaceInfo = WorkspaceInfo(workspace.id, workspace.name),
            isSharedWorkspace = true
        )
    }

    /**
     * Calls the Cloud Function to recursively delete a document or collection specified by the [path]
     *
     * @param path the path to the document/collection
     * @throws Exception on failure
     */
    private fun recursiveDelete(path: String): Task<HttpsCallableResult> {
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

    override suspend fun deleteWorkspace(workspace: Workspace): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val workspacePath = "${FirestoreCollection.WORKSPACES}/${workspace.id}"
            val boardsPath =
                "${FirestoreCollection.WORKSPACES}/${workspace.id}/${FirestoreCollection.BOARDS}"

            // delete the workspace
            recursiveDelete(workspacePath)

            // delete the workspace boards
            recursiveDelete(boardsPath)

            deleteWorkspaceFromMembers(workspace.id, workspace.members, scope = this@withContext)
        }
    }

    /**
     * Deletes the workspace information from its members.
     *
     * @param workspaceId the workspace id
     * @param members the list of workspace members
     */
    private fun deleteWorkspaceFromMembers(
        workspaceId: String,
        members: List<Workspace.WorkspaceMember>,
        scope: CoroutineScope
    ) {
        members.map { user ->
            scope.launch {
                firestore.collection(FirestoreCollection.USERS)
                    .document(user.id)
                    .update("workspaces.$workspaceId", FieldValue.delete())
            }
        }
    }

    override suspend fun createBoard(board: Board): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
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
            withContext(ioDispatcher) {
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

    override suspend fun updateBoard(oldBoard: Board, newBoard: Board): Result<Unit> =
        runCatching {
            withContext(ioDispatcher) {
                val boardUpdates = getBoardUpdates(oldBoard, newBoard)
                firestore.collection(FirestoreCollection.WORKSPACES).document(newBoard.workspace.id)
                    .collection(FirestoreCollection.BOARDS).document(newBoard.id)
                    .update(boardUpdates)
                    .getResult {
                        if (newBoard.name.isNotEmpty()) {
                            updateBoardInfoInWorkspace(newBoard)
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

    override suspend fun deleteBoard(
        board: Board
    ): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val boardRef = firestore.collection(FirestoreCollection.WORKSPACES).document()
                .collection(FirestoreCollection.BOARDS).document(board.id)

            // delete board and its task lists
            recursiveDelete(boardRef.toString())

            // delete the board information from the workspace it belongs to
            deleteBoardInfoFromWorkspace(board.workspace.id, board.id)
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

    override suspend fun getAllTags(boardId: String, workspaceId: String): Result<List<Tag>> =
        runCatching {
            withContext(ioDispatcher) {
                val boardRes = getBoard(boardId, workspaceId)
                if (boardRes is Result.Error) {
                    throw boardRes.e!!
                }

                (boardRes as Result.Success).data.tags.sortedBy { it.name }
            }
        }

    override suspend fun getTaskTags(
        task: com.example.kanbun.domain.model.Task,
        boardListId: String,
        boardListPath: String,
    ): Result<List<Tag>> = runCatching {
        withContext(ioDispatcher) {
            val boardId = boardListPath
                .substringAfter("${FirestoreCollection.BOARDS}/")
                .substringBefore("/${FirestoreCollection.TASK_LISTS}")
            val workspaceId = boardListPath
                .substringAfter("${FirestoreCollection.WORKSPACES}/")
                .substringBefore("/${FirestoreCollection.BOARDS}")

            val result = getAllTags(boardId, workspaceId)
            if (result is Result.Error) {
                throw result.e!!
            }

            val tags = (result as Result.Success).data
            val taskTags = tags.filter { it.id in task.tags }
            val taskTagIds = taskTags.map { it.id }
            Log.d(
                TAG,
                "getTaskTags: (old) tagsIds: ${task.tags}, (current) taskTagsIds: $taskTagIds"
            )

            // if the task.tags and the received taskTagIds are different, it means that some tags
            // have been deleted from the board, so we need to update the task with the relevant tags.
            if (task.tags != taskTagIds) {
                Log.d(TAG, "getTaskTags: newTags: $taskTagIds")
                updateTask(
                    oldTask = task,
                    newTask = task.copy(tags = taskTagIds),
                    boardListId = boardListId,
                    boardListPath = boardListPath
                )
            }

            taskTags
        }
    }
}