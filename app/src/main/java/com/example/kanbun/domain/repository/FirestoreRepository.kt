package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

/**
 * Interface  defining methods for Firestore interactions related to user data.
 */
interface FirestoreRepository {

    /**
     * Adds a user to Firestore.
     * @param user user instance to be added.
     * @return [Result] containing [Unit] on success, or an error message on failure.
     */
    suspend fun addUser(user: User): Result<Unit>

    /**
     * Retrieves a user from Firestore based on the [userId].
     * @param userId id of the user to retrieve.
     * @return [Result] containing the retrieved [User] on success, or an error message on failure.
     */
    suspend fun getUser(userId: String?): Result<User>

    fun getUserStream(userId: String?): Flow<User>

    suspend fun <T> updateUser(userId: String?, field: String, value: T): Result<Unit>

    suspend fun addWorkspace(user: User, workspace: Workspace): Result<Unit>

    suspend fun getWorkspace(workspaceId: String?): Result<Workspace>
}