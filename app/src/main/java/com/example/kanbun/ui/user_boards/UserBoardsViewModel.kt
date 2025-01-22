package com.example.kanbun.ui.user_boards

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.BuildConfig
import com.example.kanbun.common.AuthProvider
import com.example.kanbun.common.DrawerItem
import com.example.kanbun.common.Result
import com.example.kanbun.common.Role
import com.example.kanbun.common.TAG
import com.example.kanbun.common.ToastMessage
import com.example.kanbun.data.local.PreferenceDataStoreHelper
import com.example.kanbun.data.local.PreferenceDataStoreKeys
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.model.WorkspaceInfo
import com.example.kanbun.domain.repository.BoardRepository
import com.example.kanbun.domain.repository.UserRepository
import com.example.kanbun.domain.repository.WorkspaceRepository
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.main_activity.MainActivity
import com.example.kanbun.ui.main_activity.MainActivity.Companion.firebaseUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserBoardsViewModel @Inject constructor(
    userRepository: UserRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val boardRepository: BoardRepository,
    private val dataStore: PreferenceDataStoreHelper,
) : ViewModel() {

    private val _user = userRepository.getUserStream(firebaseUser?.uid).distinctUntilChanged()
    private val _currentWorkspace = MutableStateFlow<Workspace?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _message = MutableStateFlow<String?>(null)

    val userBoardsState: StateFlow<ViewState.UserBoardsViewState> = combine(
        _user, _currentWorkspace, _isLoading, _message
    ) { user, currentWorkspace, isLoading, message ->
        Log.d(TAG, "The state is triggered")
        ViewState.UserBoardsViewState(
            user = user,
            currentWorkspace = currentWorkspace,
            isLoading = isLoading,
            message = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ViewState.UserBoardsViewState())

    fun init() {
        getCurrentWorkspace()
    }

    fun checkUserVerification(
        nullUserCallback: () -> Unit,
        failedVerificationCallback: (String) -> Unit
    ) {
        if (firebaseUser == null) {
            nullUserCallback()
            return
        }

        val userInfo = firebaseUser?.providerData?.first { it.providerId != "firebase" }

        Log.d(
            TAG,
            "provider: ${userInfo?.providerId}, isEmailVerified: ${userInfo?.isEmailVerified}"
        )

        if (userInfo?.providerId == AuthProvider.EMAIL.providerId && firebaseUser?.isEmailVerified == false) {
            failedVerificationCallback(userInfo.providerId)
            return
        }
    }

    private fun getCurrentWorkspace() {
        viewModelScope.launch {
            val workspaceId = dataStore.getPreferenceFirst(
                PreferenceDataStoreKeys.CURRENT_WORKSPACE_ID,
                ""
            )
            Log.d(this@UserBoardsViewModel.TAG, "getCurrentWorkspace: workspaceId: $workspaceId")
            if (_currentWorkspace.value?.id == workspaceId) {
                return@launch
            }
            selectWorkspace(workspaceId)
        }
    }

    fun messageShown() {
        _message.value = null
    }

    fun signOutUser(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            MainActivity.signOut()
            Log.d(TAG, "signOut: $firebaseUser")
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.serverClientId)
                .requestEmail()
                .requestProfile()
                .build()

            val signInClient = GoogleSignIn.getClient(context, signInOptions)
            signInClient.signOut()
            dataStore.removePreference(PreferenceDataStoreKeys.CURRENT_WORKSPACE_ID)
            onSuccess()
        }
    }

    fun createWorkspace(name: String, user: User) = viewModelScope.launch {5
        val workspace = Workspace(
            name = name,
            owner = user.id,
            members = mapOf(user.id to Role.Workspace.Admin),
        )

        when (val result = workspaceRepository.createWorkspace(workspace)) {
            is Result.Success -> _message.value = ToastMessage.WORKSPACE_CREATED
            is Result.Error -> _message.value = result.message
        }
    }

    private var collectionJob: Job? = null
    private fun getWorkspace(flow: Flow<Result<Workspace?>>) {
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            flow.collectLatest { result ->
                result
                    .onSuccess { workspace ->
                        _currentWorkspace.value = workspace
                        _isLoading.value = false
                    }.onError { message, _ ->
                        _message.value = message
                        _isLoading.value = false
                    }
            }
        }
    }

    fun selectWorkspace(workspaceId: String, isPreferenceSaved: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true

            if (workspaceId == DrawerItem.SHARED_BOARDS) {
                onSharedBoardsSelected()
            } else {
                getWorkspace(workspaceRepository.getWorkspaceStream(workspaceId))
            }

            if (!isPreferenceSaved) {
                dataStore.setPreference(
                    PreferenceDataStoreKeys.CURRENT_WORKSPACE_ID,
                    workspaceId
                )
            }
        }
    }

    private suspend fun onSharedBoardsSelected() {
        _currentWorkspace.value = Workspace(
            id = DrawerItem.SHARED_BOARDS,
            name = "Shared boards",
            boards = getSharedBoards()
        )
        _isLoading.value = false
    }

    private suspend fun getSharedBoards(): List<Workspace.BoardInfo> {
        Log.d(this@UserBoardsViewModel.TAG, "getSharedBoards is called")
        var boards: List<Workspace.BoardInfo> = emptyList()
        boardRepository.getSharedBoards(
            _user.first()?.sharedBoards ?: emptyMap()
        ).onSuccess { sharedBoards ->
            boards = sharedBoards.map { board ->
                Workspace.BoardInfo(
                    boardId = board.id,
                    workspaceId = board.workspace.id,
                    name = board.name,
                    cover = board.cover
                )
            }
        }.onError { message, _ ->
            _message.value = message
            boards = emptyList()
        }

        return boards
    }

    fun createBoard(name: String, userId: String, workspace: Workspace) {
        Log.d(TAG, "createBoard is called: name: $name, workspace: $workspace")
        viewModelScope.launch {
            boardRepository.createBoard(
                Board(
                    name = name,
                    owner = userId,
                    workspace = WorkspaceInfo(workspace.id, workspace.name),
                    members = listOf(Board.BoardMember(id = userId, role = Role.Board.Admin))
                ),
            ).onSuccess {
                _message.value = "Board is successfully created"
            }.onError { message, _ ->
                _message.value = message
            }
        }
    }
}