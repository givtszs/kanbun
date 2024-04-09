package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.FirestoreRepository
import javax.inject.Inject

class UpdateUserUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val credentialsValidationUseCases: CredentialsValidationUseCases
) {
    suspend operator fun invoke(oldUser: User, newUser: User): Result<Unit> {
        val userUpdates = mutableMapOf<String, String?>()
        var error: Result.Error<Unit>? = null
        if (newUser.name != oldUser.name) {
            // validate name
            credentialsValidationUseCases.validateNameUseCase(newUser.name ?: "")
                .onSuccess {
                    userUpdates["name"] = newUser.name
                }
                .onError { message, throwable ->
                   error = Result.Error(message, throwable)
                }
        }
        if (newUser.tag != oldUser.tag) {
            // validate tag
            userUpdates["tag"] = newUser.tag
        }

        if (error != null) {
            return error as Result.Error<Unit>
        }

        return firestoreRepository.updateUser(newUser.id, userUpdates)
    }
}