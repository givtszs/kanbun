package com.example.kanbun.domain.usecase

import com.example.kanbun.common.AuthProvider
import com.example.kanbun.common.Result
import com.example.kanbun.data.toUser
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

/**
 * Use case for managing user-related operation in Firestore.
 * @property firestoreRepository repository responsible for Firestore operations.
 */
class ManageFirestoreUserUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) {

    /**
     * Saves the user information to Firestore.
     * @param user [FirebaseUser] instance.
     * @return A [Result] containing [Unit] on success, or an error message on failure.
     */
    suspend fun saveUser(user: FirebaseUser, provider: AuthProvider): Result<Unit> {
        val checkResult = firestoreRepository.getUser(user.uid)
        return if (checkResult is Result.Error) {
            firestoreRepository.addUser(user.toUser(provider))
        } else {
            Result.Error("User data is already saved")
        }
    }
}