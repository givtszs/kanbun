package com.example.kanbun.domain.usecase

import android.util.Log
import com.example.kanbun.common.Result
import com.example.kanbun.common.toFirestoreTags
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.repository.FirestoreRepository
import javax.inject.Inject

class UpdateBoardUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) {
    private val TAG = "UpdateBoardUseCase"

    suspend operator fun invoke(oldBoard: Board, newBoard: Board): Result<Unit> {
        val mapOfUpdates = mutableMapOf<String, Any>()
        if (newBoard.name != oldBoard.name) {
            mapOfUpdates["name"] = newBoard.name
        }
        if (newBoard.description != oldBoard.description) {
            mapOfUpdates["description"] = newBoard.description
        }
        if (newBoard.tags != oldBoard.tags) {
            mapOfUpdates["tags"] = newBoard.tags.toFirestoreTags()
        }

        Log.d(TAG, "mapOfUpdates: $mapOfUpdates")

        return firestoreRepository.updateBoard(newBoard, mapOfUpdates)
    }

}