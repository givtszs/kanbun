package com.example.kanbun.ui.user_boards

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.kanbun.R
import com.example.kanbun.common.AuthProvider
import com.example.kanbun.common.DrawerItem
import com.example.kanbun.common.Result
import com.example.kanbun.common.Role
import com.example.kanbun.common.ToastMessage
import com.example.kanbun.data.local.PreferenceDataStoreHelper
import com.example.kanbun.data.local.PreferenceDataStoreKeys
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.model.WorkspaceInfo
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.domain.usecase.GetUserUseCase
import com.example.kanbun.domain.utils.ConnectivityChecker
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.main_activity.MainActivity
import com.example.kanbun.ui.main_activity.MainActivity.Companion.firebaseUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "UserBoardsViewModel"

@HiltViewModel
class UserBoardsViewModel @Inject constructor(
    private val connectivityChecker: ConnectivityChecker,
    private val firestoreRepository: FirestoreRepository,
    private val dataStore: PreferenceDataStoreHelper,
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {
    // TODO: Update the code to use this code
//    private val _user =
//        firestoreRepository.getUserStream(firebaseUser?.uid ?: "").distinctUntilChanged()
    private var _user = MutableStateFlow<User?>(null)
    private val _currentWorkspace = MutableStateFlow<Workspace?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _message = MutableStateFlow<String?>(null)

    val userBoardsState: StateFlow<ViewState.UserBoardsViewState> = combine(
        _user, _currentWorkspace, _isLoading, _message
    ) { user, currentWorkspace, isLoading, message ->
        ViewState.UserBoardsViewState(
            user = user,
            currentWorkspace = currentWorkspace,
            isLoading = isLoading,
            message = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ViewState.UserBoardsViewState())

    fun init(
        navController: NavController
    ) {
        viewModelScope.launch {
            // reload FirebaseUser instance
            updateUser()
            if (firebaseUser == null) {
                _isLoading.value = false
                _message.value = "Firebase user is null"
                navController.navigate(R.id.action_userBoardsFragment_to_registrationPromptFragment)
                return@launch
            }


            val userInfo = firebaseUser?.providerData?.first { it.providerId != "firebase" }

            Log.d(
                TAG,
                "provider: ${userInfo?.providerId}, isEmailVerified: ${userInfo?.isEmailVerified}"
            )


            if (userInfo?.providerId == AuthProvider.EMAIL.providerId && firebaseUser?.isEmailVerified == false) {
                _message.value = "Complete registration by signing in with ${userInfo.providerId} and verifying your email"
                navController.navigate(UserBoardsFragmentDirections.actionUserBoardsFragmentToRegistrationPromptFragment())
                return@launch
            }

            getUser()
            getCurrentWorkspace()
        }
    }


    private suspend fun getUser() {
        when (val result = getUserUseCase()) {
            is Result.Success -> _user.value = result.data
            is Result.Error -> _message.value = result.message
        }
    }

    private suspend fun updateUser() {
        if (!connectivityChecker.hasInternetConnection()) {
            _message.value = ToastMessage.NO_NETWORK_CONNECTION
            return
        }

        firebaseUser?.reload()?.await()
    }

    private suspend fun getCurrentWorkspace() {
        val workspaceId =
            dataStore.getPreferenceFirst(PreferenceDataStoreKeys.CURRENT_WORKSPACE_ID, "")
        Log.d("UserBoardsViewModel", "getCurrentWorkspace: $workspaceId")

        if (workspaceId.isEmpty()) {
            _currentWorkspace.value = null
            _isLoading.value = false
            return
        }

        selectWorkspace(workspaceId)
    }

    fun messageShown() {
        _message.value = null
    }

    fun signOutUser(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            MainActivity.signOut()
            Log.d(TAG, "signOut: ${firebaseUser}")
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("723106455668-7apee9lsea93gpi66cjkoiom258i30e2.apps.googleusercontent.com")
                .requestEmail()
                .requestProfile()
                .build()

            val signInClient = GoogleSignIn.getClient(context, signInOptions)
            signInClient.signOut()
            dataStore.removePreference(PreferenceDataStoreKeys.CURRENT_WORKSPACE_ID)
            onSuccess()
        }
    }

    fun createWorkspace(name: String, user: User) = viewModelScope.launch {
        if (user.workspaces.any { it.name == name }) {
            _message.value = "Workspace with the same name already exists!"
            return@launch
        }

        val workspace = Workspace(
            name = name,
            owner = user.id,
            members = mapOf(user.id to Role.Workspace.Admin),
        )

        when (val result = firestoreRepository.createWorkspace(workspace)) {
            is Result.Success -> {
                _message.value = ToastMessage.WORKSPACE_CREATED
                getUser()
            }
            is Result.Error -> _message.value = result.message
        }
    }

    fun selectWorkspace(workspaceId: String, isPreferenceSaved: Boolean = true) = viewModelScope.launch {
        _isLoading.value = true

        if (workspaceId == DrawerItem.SHARED_BOARDS) {
            val sharedBoards = getSharedBoards()
            _currentWorkspace.value = Workspace(
                id = DrawerItem.SHARED_BOARDS,
                name = "Shared boards",
                boards = sharedBoards
            )
            Log.d(TAG, "_currentWorkspace: ${_currentWorkspace.value}")
            if (!isPreferenceSaved) {
                dataStore.setPreference(
                    PreferenceDataStoreKeys.CURRENT_WORKSPACE_ID,
                    DrawerItem.SHARED_BOARDS
                )
            }
            _isLoading.value = false
            return@launch
        }

        when (val result = firestoreRepository.getWorkspace(workspaceId)) {
            is Result.Success -> {
                _currentWorkspace.value = result.data
                _isLoading.value = false
                if (!isPreferenceSaved) {
                    dataStore.setPreference(
                        PreferenceDataStoreKeys.CURRENT_WORKSPACE_ID,
                        result.data.id
                    )
                }
            }

            is Result.Error -> {
                _message.value = result.message
                _isLoading.value = false
                _currentWorkspace.value = null
            }
        }
    }

    private suspend fun getSharedBoards(): List<Workspace.BoardInfo> {
        return when (val result = firestoreRepository.getSharedBoards(_user.value?.sharedBoards ?: emptyMap())) {
            is Result.Success -> result.data.map { board ->
                Workspace.BoardInfo(
                    boardId = board.id,
                    workspaceId = board.workspace.id,
                    name = board.name,
                    cover = board.cover
                )
            }

            is Result.Error -> {
                _message.value = result.message
                emptyList()
            }
        }
    }



    fun createBoard(name: String, userId: String, workspace: Workspace) = viewModelScope.launch {
        if (_currentWorkspace.value?.boards?.any { it.name == name } == true) {
            _message.value = "Board with the same name already exists!"
            return@launch
        }

        // TODO: Wrap in the `when` expression to process the result
        firestoreRepository.createBoard(
            Board(
                name = name,
                owner = userId,
                workspace = WorkspaceInfo(workspace.id, workspace.name),
                members = listOf(Board.BoardMember(id = userId, role = Role.Board.Admin))
            ),
        )
        selectWorkspace(workspace.id)
    }
}