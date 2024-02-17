package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.ui.model.TagUi
import javax.inject.Inject

class CreateTagUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) {
    suspend operator fun invoke(
        tags: List<TagUi>,
        newTag: com.example.kanbun.domain.model.Tag,
        boardPath: String,
        boardId: String
    ): Result<String> {
        val isAlreadyCreated = tags.any { it.tag.name == newTag.name }
        return if (isAlreadyCreated) {
            Result.Error("Tag with the same name is already created")
        } else {
            firestoreRepository.createTag(
                tag = newTag,
                boardId = boardId,
                boardPath = boardPath
            )
        }
    }
}