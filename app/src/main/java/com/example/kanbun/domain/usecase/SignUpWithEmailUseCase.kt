package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.repository.AuthenticationRepository
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

/**
 * A use case responsible for signing the user up with email credentials.
 *
 * @property authRepository the repository for authentication
 * @property validateEmailUseCase the use case for email validation
 * @property validatePasswordUseCase the use case for password validation
 * @property validateNameUseCase the use case for name validation
 */
class SignUpWithEmailUseCase @Inject constructor(
    private val authRepository: AuthenticationRepository,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val validateNameUseCase: ValidateNameUseCase
) {
    companion object {
        private const val TAG = "SignUpWithEmailUseCase"
    }

    suspend operator fun invoke(name: String, email: String, password: String): Result<FirebaseUser> {
        val areCredentialsValid = validateUserCredentials(name, email, password)
        if (areCredentialsValid is Result.Error) {
            return Result.Error(areCredentialsValid.message)
        }

        return authRepository.signUpWithEmail(name, email, password)
    }

    private fun validateUserCredentials(name: String, email: String, password: String): Result<Unit> {
        val isNameValid = validateNameUseCase(name)
        val isEmailValid = validateEmailUseCase(email)
        val isPasswordValid = validatePasswordUseCase(password)

        if (isNameValid is Result.Error) {
            return isNameValid
        }

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