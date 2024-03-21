package com.example.kanbun.ui.workspace_settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.data.local.PreferenceDataStoreHelper
import com.example.kanbun.data.local.PreferenceDataStoreKeys
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.domain.usecase.UpdateWorkspaceUseCase
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
    private val dataStore: PreferenceDataStoreHelper,
    private val updateWorkspaceUseCase: UpdateWorkspaceUseCase
) : ViewModel() {

    private var _workspaceSettingsState = MutableStateFlow(ViewState.WorkspaceSettingsViewState())
    val workspaceSettingsState: StateFlow<ViewState.WorkspaceSettingsViewState> = _workspaceSettingsState

    fun updateWorkspace(oldWorkspace: Workspace, newWorkspace: Workspace, onSuccess: () -> Unit) {
        if (oldWorkspace == newWorkspace) {
            onSuccess()
            return
        }

        viewModelScope.launch {
            when (val result = updateWorkspaceUseCase(oldWorkspace, newWorkspace)) {
                is Result.Success -> onSuccess()
                is Result.Error -> Log.d("WorkspaceSettingsViewModel", result.message, result.e)
                is Result.Loading -> {}
            }
        }
    }

    fun deleteWorkspaceCloudFn(workspace: Workspace, onSuccess: () -> Unit) {
        _workspaceSettingsState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = firestoreRepository.deleteWorkspace(workspace)) {
                is Result.Success ->  {
                    dataStore.setPreference(PreferenceDataStoreKeys.CURRENT_WORKSPACE_ID, "")
                    onSuccess()
                }
                is Result.Error -> Log.e("WorkspaceSettingsViewModel", result.message, result.e)
                is Result.Loading -> {}
            }
        }
    }
}