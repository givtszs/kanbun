package com.example.kanbun.data.repository

import android.util.Log
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.data.toFirestoreUser
import com.example.kanbun.data.toUser
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.FirestoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "FirestoreRepository"

class FirestoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreRepository {

    override suspend fun addUser(user: User): Result<Unit> {
        return try {
            firestore.collection(FirestoreCollection.USERS.collectionName)
                .document(user.uid)
                .set(user.toFirestoreUser())
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Exception(e.message, e)
        }
    }

    override suspend fun getUser(userId: String): Result<User> {
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
}