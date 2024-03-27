package com.example.kanbun.ui.workspace_settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.data.local.PreferenceDataStoreHelper
import com.example.kanbun.data.local.PreferenceDataStoreKeys
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkspaceSettingsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val dataStore: PreferenceDataStoreHelper,
) : ViewModel() {

    private var _members = MutableStateFlow<List<User>>(emptyList())
    private var _foundUsers = MutableStateFlow<List<User>?>(null)
    private var _message = MutableStateFlow<String?>(null)
    private var _isLoading = MutableStateFlow(false)
    val workspaceSettingsState: StateFlow<ViewState.WorkspaceSettingsViewState> =
        combine(
            _members,
            _foundUsers,
            _message,
            _isLoading
        ) { members, foundUsers, message, isLoading ->
            ViewState.WorkspaceSettingsViewState(
                members = members,
                foundUsers = foundUsers,
                message = message,
                isLoading = isLoading
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            ViewState.WorkspaceSettingsViewState()
        )

    fun init(members: List<Workspace.WorkspaceMember>) {
        viewModelScope.launch {
            val fetchedMembers = mutableListOf<User>()
            members.forEach { member ->
                when (val result = firestoreRepository.getUser(member.id)) {
                    is Result.Success -> fetchedMembers.add(result.data)
                    is Result.Error -> _message.value = result.message
                }
            }
            _members.value = fetchedMembers
            Log.d("WorkspaceSettingsViewModel", "init: ${_members.value}")
        }
    }

    fun updateWorkspace(oldWorkspace: Workspace, newWorkspace: Workspace, onSuccess: () -> Unit) {
        if (oldWorkspace == newWorkspace) {
            onSuccess()
            return
        }

        viewModelScope.launch {
            when (val result = firestoreRepository.updateWorkspace(oldWorkspace, newWorkspace)) {
                is Result.Success -> onSuccess()
                is Result.Error -> Log.d("WorkspaceSettingsViewModel", result.message, result.e)
            }
        }
    }

    fun deleteWorkspaceCloudFn(workspace: Workspace, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            when (val result = firestoreRepository.deleteWorkspace(workspace)) {
                is Result.Success -> {
                    dataStore.setPreference(PreferenceDataStoreKeys.CURRENT_WORKSPACE_ID, "")
                    onSuccess()
                }

                is Result.Error -> Log.e("WorkspaceSettingsViewModel", result.message, result.e)
            }
        }
    }

    fun searchUser(tag: String) = viewModelScope.launch {
        when (val result = firestoreRepository.findUsersByTag(tag)) {
            is Result.Success -> _foundUsers.value = result.data
            is Result.Error -> _message.value = result.message
        }
    }

    fun resetFoundUsers() {
        _foundUsers.value = null
    }

    fun removeMember(member: User) {
        _members.update { _member -> _member.filterNot { it == member } }
    }

    fun messageShown() {
        _message.value = null
    }
}