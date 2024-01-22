package com.example.kanbun.ui.root.user_boards.workspace_settings

import androidx.lifecycle.ViewModel
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WorkspaceSettingsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    suspend fun updateWorkspaceName(workspace: Workspace, newName: String): Pair<Boolean, String> {
        return when (val result = firestoreRepository.updateWorkspaceName(workspace, newName)) {
            is Result.Success -> Pair(true, "Workspace data has been updated")
            is Result.Error -> Pair(false, result.message ?: "")
        }
    }
}