package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    /**
     * Adds a new [User] entry in the Firestore database.
     *
     * @param user user instance to be added.
     * @return [Result] containing [Unit] on success, or an error message on failure.
     */
    suspend fun createUser(user: User): Result<Unit>

    /**
     * Retrieves a [User] from the Firestore database based on the given [userId].
     *
     * @param userId the id of the user to retrieve.
     * @return [Result] containing the retrieved [User] on success, or an error message on failure.
     */
    suspend fun getUser(userId: String): Result<User>

    /**
     * Retrieves a stream of [User] data for the specified [userId] from the Firestore database.
     *
     * @param userId the id of the user for which to retrieve the data stream
     * @return [Flow] that emits the [User] data for the specified [userId]
     */
    fun getUserStream(userId: String): Flow<User?>

    suspend fun updateUser(oldUser: User, newUser: User): Result<Unit>

    /**
     * Retrieves a list of [User]s for the given list of user ids
     *
     * @param userIds the list of user ids to retrieve
     * @return [Result] with the list of [User]s on success, or an error message on failure
     */
    suspend fun getUsers(userIds: List<String>): Result<List<User>>
}