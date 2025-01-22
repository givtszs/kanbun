package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.repository.AuthenticationRepository
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

/**
 * A use case responsible for signing the user in with email credentials.
 *
 * @property authRepository the repository for authentication
 * @property validateEmailUseCase the use case for email validation
 * @property validatePasswordUseCase the use case for password validation
 */
class SignInWithEmailUseCase @Inject constructor(
    private val authRepository: AuthenticationRepository,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
) {
    companion object {
        private const val TAG = "SignInWithEmailUseCase"
    }


    suspend operator fun invoke(email: String, password: String): Result<FirebaseUser> {
        val areCredentialsValid = validateUserCredentials(email, password)
        if (areCredentialsValid is Result.Error) {
            return Result.Error(areCredentialsValid.message)
        }

        return authRepository.signInWithEmail(email, password)
    }

    private fun validateUserCredentials(email: String, password: String): Result<Unit> {
        val isEmailValid = validateEmailUseCase(email)
        val isPasswordValid = validatePasswordUseCase(password)

        if (isEmailValid is Result.Error) {
            return isEmailValid
        }

        if (isPasswordValid is Result.Error) {
            return isPasswordValid
        }

        // credentials are valid
        return Result.Success(Unit)
    }
}