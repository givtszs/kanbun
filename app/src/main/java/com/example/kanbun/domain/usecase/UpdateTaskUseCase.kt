package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.repository.FirestoreRepository
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) {
    suspend operator fun invoke(
        oldTask: Task,
        boardListId: String,
        boardListPath: String,
        newTask: Task,
    ): Result<Unit> {
        val mapOfUpdates = mutableMapOf<String, Any?>()
        val taskId = newTask.id
        // TODO: Replace hardcoded `tasks` string with the enum class or constant
        if (newTask.name != oldTask.name) {
            mapOfUpdates["tasks.$taskId.name"] = newTask.name
        }
        if (newTask.description != oldTask.description) {
            mapOfUpdates["tasks.$taskId.description"] = newTask.description
        }
        if (newTask.dateStarts != oldTask.dateStarts) {
            mapOfUpdates["tasks.$taskId.dateStarts"] = newTask.dateStarts
        }
        if (newTask.dateEnds != oldTask.dateEnds) {
            mapOfUpdates["tasks.$taskId.dateEnds"] = newTask.dateEnds
        }
        if (newTask.tags != oldTask.tags) {
            mapOfUpdates["tasks.$taskId.tags"] = newTask.tags
        }

        return firestoreRepository.updateTask(newTask.id, boardListId, boardListPath, mapOfUpdates)
    }
}