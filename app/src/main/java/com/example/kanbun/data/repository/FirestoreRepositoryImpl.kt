package com.example.kanbun.data.repository

import android.util.Log
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.mapToFirestoreWorkspaces
import com.example.kanbun.common.toFirestoreUser
import com.example.kanbun.common.toFirestoreWorkspace
import com.example.kanbun.common.toUser
import com.example.kanbun.common.toWorkspace
import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.data.model.FirestoreWorkspace
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.UserWorkspace
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.repository.FirestoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "FirestoreRepository"

class FirestoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreRepository {

    override suspend fun addUser(user: User): Result<Unit> {
        return try {
            firestore.collection(FirestoreCollection.USERS.collectionName)
                .document(user.id)
                .set(user.toFirestoreUser())
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Exception(e.message, e)
        }
    }

    override suspend fun getUser(userId: String?): Result<User> {
        if (userId == null) {
            return Result.Error("User ID is null")
        }

        return try {

            val task = firestore.collection(FirestoreCollection.USERS.collectionName)
                .document(userId)
                .get()
                .await()

            Log.d(TAG, "task: $task")

            val firestoreUser = task.toObject(FirestoreUser::class.java)
            Log.d(TAG, "firestoreUser: $firestoreUser")

            if (firestoreUser != null) {
                Result.Success(firestoreUser.toUser(userId))
            } else {
                Result.Error("Requested document does not exist")
            }
        } catch (e: Exception) {
            Result.Exception(e.message, e)
        }
    }

    override fun getUserStream(userId: String?): Flow<User> {
        if (userId == null) {
            return emptyFlow()
        }

        return callbackFlow {
            val listener = firestore.collection(FirestoreCollection.USERS.collectionName)
                .document(userId)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        close(exception)
                        return@addSnapshotListener
                    }
                    Log.d(TAG, "Data update: ${snapshot?.data}")
                    val data = snapshot?.data
                    data?.let { values ->
                        trySend(
                            FirestoreUser(
                                email = values["email"] as String,
                                name = values["name"] as String?,
                                profilePicture = values["profilePicture"] as String?,
                                authProvider = values["authProvider"] as String,
                                workspaces = values["workspaces"] as List<Map<String, String>>,
                                cards = values["cards"] as List<String>
                            ).toUser(userId)
                        )
                    }
                }

            awaitClose {
                listener.remove()
            }
        }
    }

    override suspend fun <T> updateUser(userId: String?, field: String, value: T): Result<Unit> {
        if (userId == null) {
            return Result.Error("User ID is null")
        }

        val mappedValue = when (field) {
            "name" -> value
            "workspaces" -> (value as List<UserWorkspace>).mapToFirestoreWorkspaces()
            else -> Unit
        }

        Log.d(TAG, "Mapped value: $mappedValue")

        return try {
            firestore.collection(FirestoreCollection.USERS.collectionName)
                .document(userId)
                .update(field, mappedValue)
                .addOnSuccessListener {
                    Log.d(TAG, "User's `$field` has been updated")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Couldn't update user's `$field` value: ${e.message}", e)
                }
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Exception(e.message, e)
        }
    }

    override suspend fun addWorkspace(user: User, workspace: Workspace): Result<String> {
        return try {
            val task = firestore.collection(FirestoreCollection.WORKSPACES.collectionName)
                .add(workspace.toFirestoreWorkspace())
                .addOnSuccessListener { docRef ->
                    Log.d(TAG, "Workspace has been added! ID: ${docRef.id}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, e.message, e)
                }
            task.await()

            if (task.isSuccessful) {
                Log.d(TAG, "task is successful: ${task.result.path}")
                val updRes = updateUser(
                    user.id,
                    "workspaces",
                    user.workspaces + UserWorkspace(task.result.id, workspace.name)
                )

                if (updRes !is Result.Success) {
                    Result.Error("Couldn't add workspace")
                } else {
                    Result.Success(task.result.id)
                }
            } else {
                Result.Error("Couldn't add a workspace: ${task.exception?.message}")
            }
        } catch (e: Exception) {
            Result.Exception(e.message, e)
        }
    }

    override suspend fun getWorkspace(workspaceId: String?): Result<Workspace> {
        if (workspaceId.isNullOrEmpty()) {
            return Result.Error("Workspace ID is null or empty")
        }

        return try {
            val task = firestore.collection(FirestoreCollection.WORKSPACES.collectionName)
                .document(workspaceId)
                .get()
                .await()
            Log.d(TAG, "task: $task, data: ${task.data?.values}")

            val firestoreWorkspace = task.toObject(FirestoreWorkspace::class.java)
            Log.d(TAG, "workspace: $firestoreWorkspace")

            if (firestoreWorkspace != null) {
                Result.Success(firestoreWorkspace.toWorkspace(workspaceId))
            } else {
                Result.Error("Requested document does not exist")
            }
        } catch (e: Exception) {
            Result.Exception(e.message, e)
        }
    }

    override suspend fun <T> updateWorkspace(
        workspaceId: String?,
        field: String,
        value: T
    ): Result<Unit> {
        if (workspaceId.isNullOrEmpty()) {
            return Result.Error("Workspace ID is null or empty")
        }

        val mappedValue = when (field) {
            "name" -> value
            else -> Unit
        }

        return try {
            val task = firestore.collection(FirestoreCollection.WORKSPACES.collectionName)
                .document(workspaceId)
                .update(field, mappedValue)
            task.await()
            if (task.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Couldn't update a workspace $workspaceId: ${task.exception?.message}")
            }
        } catch (e: Exception) {
            Result.Exception(e.message, e)
        }
    }
}