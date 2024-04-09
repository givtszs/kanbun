package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result

class ValidateNameUseCase {
    operator fun invoke(name: String): Result<Unit> {
        val nameRegex = Regex("[a-zA-Z ]+")
        return when {
            name.isEmpty() -> Result.Error("Name is required")
            name.length < 2 -> Result.Error("Name should be at least 2 characters long")
            name.length > 124 -> Result.Error("Name should be 124 characters or less")
            !name.matches(nameRegex) -> Result.Error("Name should contain only letters")
            else -> Result.Success(Unit)
        }
    }
}