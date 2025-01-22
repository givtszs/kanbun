package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.utils.EmailPatternValidator
import javax.inject.Inject

/**
 * A use case class responsible for validating an email string.
 */
class ValidateEmailUseCase @Inject constructor(
    private val emailPatternValidator: EmailPatternValidator
) {

    /**
     * Validates the given [email] address.
     *
     * @param email the email to be validated.
     * @return [Result.Success] if the email is valid, [Result.Error] if the email is invalid.
     */
    operator fun invoke(email: String): Result<Unit> {
        return when {
            email.isEmpty() -> Result.Error( "Email cannot be empty")
            !emailPatternValidator.isEmailPatternValid(email) -> Result.Error("Invalid email format")
            email.substringBefore("@").endsWith(".") ||
                    email.substringBefore("@").startsWith(".") ->
                Result.Error("Email cannot start or end with `.`")
            else -> Result.Success(Unit)
        }
    }
}