package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
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
    suspend fun saveUser(user: FirebaseUser): Result<Unit> {
        return firestoreRepository.addUser(
            User(
                uid = user.uid,
                email = user.email!!,
                name = user.displayName,
                profilePicture = user.photoUrl?.toString(),
                authProviders = user.providerData.map { it.providerId }.filterNot { it == "firebase" },
                workspaces = emptyList(),
                cards = emptyList()
            )
        )
    }
}