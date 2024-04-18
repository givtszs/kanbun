package com.example.kanbun.ui.board_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.common.Role
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.domain.repository.UserRepository
import com.example.kanbun.domain.repository.WorkspaceRepository
import com.example.kanbun.domain.usecase.SearchUserUseCase
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.model.Member
import com.example.kanbun.ui.model.TagUi
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
class BoardSettingsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val userRepository: UserRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val searchUserUseCase: SearchUserUseCase
) : ViewModel() {
    companion object {
        private const val TAG = "BoardSettingsViewModel"
    }

    // preserves the list of workspace members of type WorkspaceMember
    private var _workspaceMembers = MutableStateFlow<List<User>>(emptyList())

    private var _isLoading = MutableStateFlow(false)
    private var _message = MutableStateFlow<String?>(null)
    private var _tags = MutableStateFlow<List<Tag>>(emptyList())
    private var _boardMembers = MutableStateFlow<List<Member>>(emptyList())
    private var _foundUsers = MutableStateFlow<List<UserSearchResult>?>(null)

    val boardSettingsState: StateFlow<ViewState.BoardSettingsViewState> =
        combine(
            _isLoading,
            _message,
            _tags,
            _boardMembers,
            _foundUsers
        ) { isLoading, message, tags, boardMembers, foundUsers ->
            ViewState.BoardSettingsViewState(
                isLoading = isLoading,
                message = message,
                tags = tags.map { TagUi(it, false) },
                boardMembers = boardMembers,
                foundUsers = foundUsers
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            ViewState.BoardSettingsViewState()
        )

    fun init(tags: List<Tag>, members: List<User>, boardMembers: List<Board.BoardMember>, workspaceId: String) {
        _tags.value = tags

        val boardMembersById = boardMembers.associateBy { it.id }
        _boardMembers.value = members.map { Member(user = it, role = boardMembersById[it.id]?.role) }

        fetchWorkspaceMembers(workspaceId)
    }

    private fun fetchWorkspaceMembers(workspaceId: String) {
        viewModelScope.launch {
            when (val resultWorkspace = workspaceRepository.getWorkspace(workspaceId)) {
                is Result.Success -> {
                    val workspace = resultWorkspace.data
                    when (val resultMembers =
                        userRepository.getUsers(workspace.members.keys.toList())
                    ) {
                        is Result.Success -> _workspaceMembers.value = resultMembers.data
                        is Result.Error -> _message.value = resultMembers.message
                    }
                }

                is Result.Error -> _message.value = resultWorkspace.message
            }
        }
    }

    fun deleteBoard(board: Board, onSuccess: () -> Unit) =
        viewModelScope.launch {
            _isLoading.value = true
            processResult(firestoreRepository.deleteBoard(board), onSuccess)
        }

    fun updateBoard(oldBoard: Board, newBoard: Board, onSuccess: () -> Unit) =
        viewModelScope.launch {
            processResult(firestoreRepository.updateBoard(oldBoard, newBoard), onSuccess)
        }

    private fun <T : Any> processResult(result: Result<T>, onSuccess: () -> Unit) {
        when (result) {
            is Result.Success -> onSuccess()
            is Result.Error -> _message.value = result.message
        }
    }

    fun messageShown() {
        _message.value = null
    }

    fun setTags(tags: List<Tag>) {
        if (_tags.value != tags) {
            _tags.value = tags
        }
    }

    fun searchUser(tag: String) = viewModelScope.launch {
        when (val result = searchUserUseCase(tag)) {
            is Result.Success -> _foundUsers.value = result.data.map { user ->
                UserSearchResult(user, _boardMembers.value.any { it.user.id == user.id })
            }
            is Result.Error -> _message.value = result.message
        }
    }

    fun resetFoundUsers(clear: Boolean = false) {
        _foundUsers.value = if (clear) {
            null
        } else {
            _workspaceMembers.value.map { user ->
                UserSearchResult(user, _boardMembers.value.any { it.user.id == user.id })
            }
        }
    }

    fun addMember(user: User) {
        if (!_boardMembers.value.any { it.user.id == user.id } ) {
            _boardMembers.update { it + Member(user, Role.Board.Member) }
        }
    }

    fun removeMember(member: User) {
        _boardMembers.update { _member -> _member.filterNot { it.user.id == member.id } }
    }

    fun setMembers(members: List<Member>) {
        _boardMembers.value = members
    }

}