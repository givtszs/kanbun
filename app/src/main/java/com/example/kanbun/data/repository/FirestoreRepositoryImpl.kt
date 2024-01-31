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
import com.example.kanbun.common.toFirestoreUser
import com.example.kanbun.common.toFirestoreWorkspace
import com.example.kanbun.common.toFirestoreTask
import com.example.kanbun.common.toUser
import com.example.kanbun.common.toWorkspace
import com.example.kanbun.data.model.FirestoreBoard
import com.example.kanbun.data.model.FirestoreBoardList
import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.data.model.FirestoreWorkspace
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.repository.FirestoreRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
        workspaceInfo: User.WorkspaceInfo
    ): Result<Unit> =
        runCatching {
            firestore.collection(FirestoreCollection.USERS.collectionName)
                .document(userId)
                .update("workspaces.${workspaceInfo.id}", workspaceInfo.name)
                .await()
        }

    private suspend fun deleteUserWorkspace(userId: String, workspaceId: String): Result<Unit> =
        runCatching {
            firestore.collection(FirestoreCollection.USERS.collectionName)
                .document(userId)
                .update("workspaces.$workspaceId", FieldValue.delete())
                .await()
        }

    override suspend fun createWorkspace(workspace: Workspace): Result<String> =
        runCatching {
            firestore.collection(FirestoreCollection.WORKSPACES.collectionName)
                .add(workspace.toFirestoreWorkspace())
                .getResult {
                    // add the newly create workspace into the user's workspaces
                    val res = addWorkspaceInfoToUser(
                        workspace.owner,
                        User.WorkspaceInfo(result.id, workspace.name)
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
            User.WorkspaceInfo(workspace.id, workspace.name)
        )

        if (userUpdateResult is Result.Error) {
            throw userUpdateResult.e!!
        }
    }

    override suspend fun deleteWorkspace(workspace: Workspace): Result<Unit> = runCatching {
        firestore.collection(FirestoreCollection.WORKSPACES.collectionName)
            .document(workspace.id)
            .delete()
            .getResult {
                workspace.members.forEach { member ->
                    deleteUserWorkspace(member.id, workspace.id)
                }
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

    override suspend fun createBoard(board: Board): Result<String> = runCatching {
        val workspaceId = board.settings.workspace.id
        firestore.collection("${FirestoreCollection.WORKSPACES.collectionName}/$workspaceId/${FirestoreCollection.BOARDS.collectionName}")
            .add(board.toFirestoreBoard())
            .getResult {
                val workspaceUpdResult = addBoardInfoToWorkspace(
                    workspaceId,
                    Workspace.BoardInfo(
                        boardId = result.id,
                        workspaceId = workspaceId,
                        name = board.settings.name,
                        cover = board.settings.cover
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

    private suspend fun updateBoardsList(board: Board, boardListId: String): Result<Unit> =
        runCatching {
            val workspacePath =
                "${FirestoreCollection.WORKSPACES.collectionName}/${board.settings.workspace.id}"
            firestore.collection("$workspacePath/${FirestoreCollection.BOARDS.collectionName}")
                .document(board.id)
                .update("lists", board.lists + boardListId)
                .await()
        }

    override suspend fun createBoardList(
        boardList: BoardList,
        board: Board
    ): Result<String> = runCatching {
        val workspacePath =
            "${FirestoreCollection.WORKSPACES.collectionName}/${board.settings.workspace.id}"
        val boardPath = "${FirestoreCollection.BOARDS.collectionName}/${board.id}"
        firestore.collection("$workspacePath/$boardPath/${FirestoreCollection.BOARD_LIST.collectionName}")
            .add(boardList.toFirestoreBoardList())
            .getResult {
                val updateResult = updateBoardsList(board, result.id)
                if (updateResult is Result.Error) {
                    throw updateResult.e!!
                }

                result.id
            }
    }

    override fun getBoardListsStream(
        boardId: String,
        workspaceId: String
    ): Flow<Result<List<BoardList>>> = callbackFlow {
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
                            Log.d(TAG, "getBoardListsFlow#boardList: $boardList")
                            boardList
                        }.reversed()
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
        boardId: String,
        workspaceId: String
    ): Result<String> = runCatching {
        val workspacePath = "${FirestoreCollection.WORKSPACES.collectionName}/$workspaceId"
        val boardPath = "${FirestoreCollection.BOARDS.collectionName}/$boardId"
        val listPath = "${FirestoreCollection.BOARD_LIST.collectionName}"
        val taskId = UUID.randomUUID().toString()
        firestore
            .collection("$workspacePath/$boardPath/$listPath")
            .document(listId)
            .update("tasks.${taskId}", task.toFirestoreTask())
            .getResult { taskId }
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

    override suspend fun rearrangeTasksPositions(
        listPath: String,
        listId: String,
        tasks: List<com.example.kanbun.domain.model.Task>,
        from: Int,
        to: Int
    ): Result<Unit> = runCatching {
        val updatesMap = rearrange(tasks, from, to)
        updateTasksPositions(listPath, listId, updatesMap)
//        firestore.collection(listPath)
//            .document(listId)
//            .update(updatesMap)
//            .await()
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

        updMap["tasks.${task.id}"] = task.toFirestoreTask()

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
        updateTasksPositions(listPath, listId, updatesMap)
    }
}
