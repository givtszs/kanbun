package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.ui.model.TagUi
import javax.inject.Inject

class CreateTagUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) {
    suspend operator fun invoke(
        tag: Tag,
        tags: List<TagUi>,
        boardPath: String,
        boardId: String
    ): Result<String> {
        val isAlreadyCreated = tags.any { it.tag.name == tag.name }
        return if (isAlreadyCreated) {
            Result.Error("Tag with the same name is already created")
        } else {
            firestoreRepository.createTag(
                tag = tag,
                boardId = boardId,
                boardPath = boardPath
            )
        }
    }
}