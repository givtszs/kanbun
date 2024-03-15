package com.example.kanbun.ui.workspace_settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.data.local.PreferenceDataStoreHelper
import com.example.kanbun.data.local.PreferenceDataStoreKeys
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkspaceSettingsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val dataStore: PreferenceDataStoreHelper
) : ViewModel() {

    private var _workspaceSettingsState = MutableStateFlow(ViewState.WorkspaceSettingsViewState())
    val workspaceSettingsState: StateFlow<ViewState.WorkspaceSettingsViewState> = _workspaceSettingsState

    suspend fun updateWorkspaceName(workspace: Workspace, newName: String): Pair<Boolean, String> {
        return when (val result = firestoreRepository.updateWorkspaceName(workspace, newName)) {
            is Result.Success -> Pair(true, "Workspace data has been updated")
            is Result.Error -> Pair(false, result.message ?: "")
            is Result.Loading -> Pair(false, "Loading...")
        }
    }

    suspend fun deleteWorkspace(workspace: Workspace) {
        firestoreRepository.deleteWorkspace(workspace)
        dataStore.setPreference(PreferenceDataStoreKeys.CURRENT_WORKSPACE_ID, "")
    }

    fun deleteWorkspaceCloudFn(workspace: Workspace, onSuccess: () -> Unit) {
        _workspaceSettingsState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = firestoreRepository.deleteWorkspaceCloudFn(workspace)) {
                is Result.Success ->  onSuccess()
                is Result.Error -> Log.e("WorkspaceSettingsViewModel", result.message, result.e)
                is Result.Loading -> {}
            }
        }
    }
}