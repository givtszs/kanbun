package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.UserRepository
import javax.inject.Inject

class UpdateUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val validateNameUseCase: ValidateNameUseCase,
    private val validateTagUseCase: ValidateTagUseCase
) {
    suspend operator fun invoke(oldUser: User, newUser: User): Result<Unit> {
        val userUpdates = mutableMapOf<String, String?>()
        var error: Result.Error<Unit>? = null

        if (newUser.name != oldUser.name) {
            validateNameUseCase(newUser.name ?: "")
                .onError { message, throwable ->
                   error = Result.Error(message, throwable)
                }
        }

        if (newUser.tag != oldUser.tag) {
            validateTagUseCase(newUser.tag)
                .onError { message, throwable ->
                    error = Result.Error(message, throwable)
                }
        }

        if (error != null) {
            return error as Result.Error<Unit>
        }

        return userRepository.updateUser(oldUser, newUser)
    }
}