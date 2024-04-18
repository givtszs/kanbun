package com.example.kanbun.data.repository

import android.util.Log
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.TAG
import com.example.kanbun.common.getResult
import com.example.kanbun.common.runCatching
import com.example.kanbun.common.toFirestoreUser
import com.example.kanbun.common.toUser
import com.example.kanbun.data.model.FirestoreUser
import com.example.kanbun.di.IoDispatcher
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : UserRepository {

    override suspend fun createUser(user: User): Result<Unit> = runCatching {
        withContext(dispatcher) {
            firestore.collection(FirestoreCollection.USERS)
                .document(user.id)
                .set(user.toFirestoreUser())
        }
    }

    override suspend fun getUser(userId: String): Result<User> = runCatching {
        withContext(dispatcher) {
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

    override suspend fun updateUser(oldUser: User, newUser: User): Result<Unit> = runCatching {
        val userUpdates = getUserUpdates(oldUser, newUser)
        withContext(dispatcher) {
            firestore.collection(FirestoreCollection.USERS)
                .document(newUser.id)
                .update(userUpdates)
        }
    }

    private fun getUserUpdates(oldUser: User, newUser: User): Map<String, Any?> {
        val mapOfUpdates = mutableMapOf<String, Any?>()
        fun updateIfChanged(field: String, oldValue: Any?, newValue: Any?) {
            if (oldValue != newValue) {
                mapOfUpdates[field] = newValue
            }
        }

        updateIfChanged("name", oldUser.name, newUser.name)
        updateIfChanged("tag", oldUser.tag, newUser.tag)
        updateIfChanged("profilePicture", oldUser.profilePicture, newUser.profilePicture)
        return mapOfUpdates
    }

    override suspend fun getUsers(userIds: List<String>): Result<List<User>> = runCatching {
        val members = mutableListOf<User>()
        val fetchedUsers  = withContext(dispatcher) {
            userIds.map { id ->
                async {
                    getUser(id)
                }
            }
        }.awaitAll()
        fetchedUsers.forEach { result ->
            if (result is Result.Success) {
                members.add(result.data)
            }
        }
        members
    }
}