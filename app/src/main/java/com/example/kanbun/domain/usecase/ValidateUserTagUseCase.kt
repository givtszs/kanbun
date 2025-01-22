package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.repository.UserRepository
import javax.inject.Inject

class ValidateUserTagUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(tag: String): Result<Unit> {
        val isValid = validateTag(tag)
        if (isValid is Result.Error) {
            return isValid
        }

        val isTagTakenResult = userRepository.isUserTagTaken(tag)
        if (isTagTakenResult is Result.Error) {
            return Result.Error("Could not verify the tag. Try again later")
        }

        val isTagTaken = (isTagTakenResult as Result.Success).data
        if (isTagTaken) {
            return Result.Error("Tag is already taken")
        }

        return Result.Success(Unit)
    }

    private fun validateTag(tag: String): Result<Unit> {
        val tagRegex = Regex("[a-zA-Z0-9_]+")
        return when {
            tag.isEmpty() -> Result.Error("Tag is required")
            tag.any { it.isWhitespace() } -> Result.Error("Tag must not contain whitespaces")
            tag.none { it.isLetter() } -> Result.Error("Tag must contain at least 1 letter")
            tag.length < 3 -> Result.Error("Tag must be at least 3 characters long")
            tag.length > 64 -> Result.Error("Tag must be 64 characters or less")
            !tag.matches(tagRegex) -> Result.Error("Tag must contain only letters, numbers and underscores")
            else -> Result.Success(Unit)
        }
    }
}