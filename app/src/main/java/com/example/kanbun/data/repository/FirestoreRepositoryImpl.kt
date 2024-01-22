package com.example.kanbun.data.repository

import android.util.Log
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.WorkspaceRole
import com.example.kanbun.common.runCatching
import com.example.kanbun.common.toFirestoreUser
import com.example.kanbun.common.toFirestoreWorkspace
import com.example.kanbun.common.toUser
import com.example.kanbun.common.toWorkspace
import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.data.model.FirestoreWorkspace
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

    override suspend fun addUser(user: User): Result<Unit> = runCatching {
        firestore.collection(FirestoreCollection.USERS.collectionName)
            .document(user.id)
            .set(user.toFirestoreUser())
            .getResult {}
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

    private suspend fun addWorkspaceToUser(
        userId: String,
        workspaceId: String,
        workspaceName: String
    ): Result<Unit> =
        runCatching {
            firestore.collection(FirestoreCollection.USERS.collectionName)
                .document(userId)
                .update("workspaces.$workspaceId", workspaceName)
                .getResult {}
        }

    private suspend fun deleteUserWorkspace(userId: String, workspaceId: String): Result<Unit> =
        runCatching {
            firestore.collection(FirestoreCollection.USERS.collectionName)
                .document(userId)
                .update("workspaces.$workspaceId", FieldValue.delete())
                .getResult {}
        }

    override suspend fun addWorkspace(userId: String, workspace: Workspace): Result<String> =
        runCatching {
            firestore.collection(FirestoreCollection.WORKSPACES.collectionName)
                .add(workspace.toFirestoreWorkspace())
                .getResult {
                    // add the newly create workspace into the user's workspaces
                    val res = addWorkspaceToUser(userId, result.id, workspace.name)

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
                        Log.d(TAG, "getWorkspaceStream: error occured: ${error.message}")
                        trySend(null)
                        close(error)
                        return@addSnapshotListener
                    }

                    val workspace = docSnapshot?.toObject(FirestoreWorkspace::class.java)?.toWorkspace(workspaceId) ?:  throw NullPointerException("Couldn't convert FirestoreWorkspace to Workspace since the value is null")
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
                            .getResult {}
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
            .getResult {}
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
        val userUpdateResult = addWorkspaceToUser(
            user.id,
            workspace.id,
            workspace.name
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
}