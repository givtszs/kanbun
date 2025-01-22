package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.repository.BoardRepository
import javax.inject.Inject

class UpsertTagUseCase @Inject constructor(
    private val boardRepository: BoardRepository
) {
    suspend operator fun invoke(
        tag: Tag,
        tags: List<Tag>,
        boardPath: String,
        boardId: String
    ): Result<Tag> {
        val doesTagExist = tags.any { it.name == tag.name && it.id != tag.id }
        return if (doesTagExist) {
            Result.Error("Tag with the same name is already created")
        } else {
            boardRepository.upsertTag(
                tag = tag,
                boardId = boardId,
                boardPath = boardPath
            )
        }
    }
}