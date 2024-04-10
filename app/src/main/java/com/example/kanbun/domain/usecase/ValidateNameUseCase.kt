package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result

/**
 * A use case class responsible for validating a name string based on certain rules.
 *
 * The validation rules applied to the name string are:
 * - The name cannot be empty.
 * - The name must be at least 2 characters long.
 * - The name must be 124 characters or less.
 * - The name must contain only letters and whitespaces.
 */
class ValidateNameUseCase {

    /**
     * Validates the given [name] based on the defined rules.
     *
     * @param name the name to be validated.
     * @return [Result.Success] if the name is valid, [Result.Error] if the name is invalid.
     */
    operator fun invoke(name: String): Result<Unit> {
        val nameRegex = Regex("[a-zA-Z ]+")
        return when {
            name.isEmpty() -> Result.Error("Name is required")
            name.length < 2 -> Result.Error("Name must be at least 2 characters long")
            name.length > 124 -> Result.Error("Name must be 124 characters or less")
            !name.matches(nameRegex) -> Result.Error("Name must contain only letters")
            else -> Result.Success(Unit)
        }
    }
}