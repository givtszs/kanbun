package com.example.kanbun.ui.workspace_settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.data.local.PreferenceDataStoreHelper
import com.example.kanbun.data.local.PreferenceDataStoreKeys
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkspaceSettingsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val dataStore: PreferenceDataStoreHelper
) : ViewModel() {

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
//        viewModelScope.launch {
//        }
        when (val result = firestoreRepository.deleteWorkspaceCloudFn(workspace)) {
            is Result.Success -> {
                onSuccess()
            }

            is Result.Error -> Log.e("WorkspaceSettingsViewModel", result.message, result.e)
            is Result.Loading -> {}
        }
    }
}