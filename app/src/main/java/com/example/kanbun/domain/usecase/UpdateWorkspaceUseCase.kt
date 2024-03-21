package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.common.toFirestoreMembers
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.repository.FirestoreRepository
import javax.inject.Inject

class UpdateWorkspaceUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) {
    suspend operator fun invoke(oldWorkspace: Workspace, newWorkspace: Workspace): Result<Unit> {
        val mapOfUpdates = mutableMapOf<String, Any>()
        if (newWorkspace.name != oldWorkspace.name) {
            mapOfUpdates["name"] = newWorkspace.name
        }
//        if (newWorkspace.members != oldWorkspace.members) {
//            mapOfUpdates["members"] = newWorkspace.members.toFirestoreMembers()
//        }
        return firestoreRepository.updateWorkspace(newWorkspace, mapOfUpdates)
    }
}