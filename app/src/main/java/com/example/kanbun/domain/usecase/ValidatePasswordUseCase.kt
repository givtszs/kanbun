package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result


/**
 * A use case class responsible for validating a password string based on certain rules.
 *
 * The validation rules applied to the password are:
 * - The password cannot be empty
 * - The password must be at least 6 characters long
 * - The password must not contain whitespaces
 * - The password must contain at least 1 digit
 * - The password must contain at least 1 letter
 * - The password must contain at least 1 uppercase letter
 * - The password must contain at least 1 lowercase letter
 * - The password must contain at least 1 special character: ~`!@#$%^&*()_-+={}[]|\:;"'<,>.?/
 */
class ValidatePasswordUseCase {

    /**
     * Validates the given [password] based on the defined rules.
     *
     * @param password the password to be validated.
     * @return [Result.Success] if the password is valid, [Result.Error] if the password is invalid.
     */
    operator fun invoke(password: String): Result<Unit> {
        val passwordRegex = Regex("[~`!@#\$%^&*()_\\-+={}\\[\\]|\\\\:;\"'<,>.?/]")
        return when {
            password.isEmpty() -> Result.Error("Password cannot be empty")
            password.length < 6 -> Result.Error("Password must be at least 6 characters long")
//            password.length > 64 -> Result.Error("Password cannot be longer that 64 characters")
            password.any { it.isWhitespace() } -> Result.Error("Password must not contain whitespaces")
            password.none { it.isDigit() } -> Result.Error("Password must contain at least 1 digit")
            password.none { it.isLetter() } -> Result.Error("Password must contain at least 1 letter")
            password.none { it.isUpperCase() } -> Result.Error("Password must contain at least 1 uppercase letter")
            password.none { it.isLowerCase() } -> Result.Error("Password must contain at least 1 lowercase letter")
            !passwordRegex.containsMatchIn(password) -> Result.Error("Password must contain at least 1 special character: ~`!@#\$%^&*()_-+={}[]|\\:;\"'<,>.?/")
            else -> Result.Success(Unit) // Validation successful
        }
    }
}