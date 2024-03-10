package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.repository.FirestoreRepository
import javax.inject.Inject

class CreateTagUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) {
    suspend operator fun invoke(
        tag: Tag,
        tags: List<Tag>,
        boardPath: String,
        boardId: String
    ): Result<Tag> {
        val doesTagExist = tags.any { it.name == tag.name }
        return if (doesTagExist) {
            Result.Error("Tag with the same name is already created")
        } else {
            firestoreRepository.upsertTag(
                tag = tag,
                boardId = boardId,
                boardPath = boardPath
            )
        }
    }
}