package com.example.kanbun.ui.workspace_settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.common.Role
import com.example.kanbun.common.TAG
import com.example.kanbun.common.moveOwnerToTheTop
import com.example.kanbun.data.local.PreferenceDataStoreHelper
import com.example.kanbun.data.local.PreferenceDataStoreKeys
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.repository.UserRepository
import com.example.kanbun.domain.repository.WorkspaceRepository
import com.example.kanbun.domain.usecase.SearchUserUseCase
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.model.Member
import com.example.kanbun.ui.model.UserSearchResult
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
    private val userRepository: UserRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val searchUserUseCase: SearchUserUseCase,
    private val dataStore: PreferenceDataStoreHelper,
) : ViewModel() {

    // holds the list of workspace members of type User to display on the UI
    private var _members = MutableStateFlow<List<Member>>(emptyList())
    private var _foundUsers = MutableStateFlow<List<UserSearchResult>?>(null)
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

    fun init(ownerId: String, workspaceMembers: Map<String, Role.Workspace>) {
        fetchMembers(ownerId, workspaceMembers)
    }

    private fun fetchMembers(ownerId: String, members: Map<String, Role.Workspace>) {
        viewModelScope.launch {
            when (val result = userRepository.getUsers(members.keys.toList())
            ) {
                is Result.Success -> {
                    val fetchedMembers = result.data.map { user ->
                        val role = members[user.id] ?: Role.Workspace.Member
                        Member(user = user, role = role)
                    }.toMutableList()
                    moveOwnerToTheTop(ownerId, fetchedMembers)
                    _members.value = fetchedMembers
                }

                is Result.Error -> _message.value = result.message
            }
        }
    }

    fun updateWorkspace(oldWorkspace: Workspace, newWorkspace: Workspace, onSuccess: () -> Unit) {
        if (oldWorkspace == newWorkspace) {
            onSuccess()
            return
        }

        viewModelScope.launch {
            when (val result = workspaceRepository.updateWorkspace(oldWorkspace, newWorkspace)) {
                is Result.Success -> onSuccess()
                is Result.Error -> Log.d("WorkspaceSettingsViewModel", result.message, result.e)
            }
        }
    }

    fun deleteWorkspaceCloudFn(workspace: Workspace, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            when (val result = workspaceRepository.deleteWorkspace(workspace)) {
                is Result.Success -> {
                    dataStore.setPreference(PreferenceDataStoreKeys.CURRENT_WORKSPACE_ID, "")
                    onSuccess()
                }

                is Result.Error -> Log.e("WorkspaceSettingsViewModel", result.message, result.e)
            }
        }
    }

    fun searchUser(tag: String) = viewModelScope.launch {
        when (val result = searchUserUseCase(tag)) {
            is Result.Success -> _foundUsers.value = result.data.map { user ->
                UserSearchResult(user, _members.value.any { it.user.id == user.id })
            }

            is Result.Error -> _message.value = result.message
        }
    }

    fun resetFoundUsers() {
        _foundUsers.value = null
    }

    fun addMember(user: User) {
        if (!_members.value.any { it.user.id == user.id }) {
            _members.update { it + Member(user, Role.Workspace.Member) }
        }
    }

    fun removeMember(member: User) {
        _members.update {
            it.filterNot { _member ->
                _member.user.id == member.id
            }
        }
    }

    fun setMembers(members: List<Member>) {
        if (members != _members.value) {
            _members.value = members
        }
    }

    fun messageShown() {
        _message.value = null
    }
}