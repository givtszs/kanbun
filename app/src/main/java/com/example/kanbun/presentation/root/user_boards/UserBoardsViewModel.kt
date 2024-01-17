package com.example.kanbun.presentation.root.user_boards

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.ToastMessage
import com.example.kanbun.common.WorkspaceRole
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.model.WorkspaceMember
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.domain.utils.ConnectivityChecker
import com.example.kanbun.presentation.BaseViewModel
import com.example.kanbun.presentation.ViewState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "UserBoardsViewModel"

@HiltViewModel
class UserBoardsViewModel @Inject constructor(
    private val connectivityChecker: ConnectivityChecker,
    private val firestoreRepository: FirestoreRepository
) : BaseViewModel() {

    private val _user = firestoreRepository.getUserStream(firebaseUser?.uid).distinctUntilChanged()
    private val _currentWorkspace = MutableStateFlow<Workspace>(Workspace())
    private val _isLoading = MutableStateFlow(false)
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

    fun messageShown() {
        _message.value = null
    }

    suspend fun updateUser() {
        if (!connectivityChecker.hasInternetConnection()) {
            _message.value = ToastMessage.NO_NETWORK_CONNECTION
            return
        }

        firebaseUser?.reload()?.await()
    }

    fun signOutUser(context: Context) {
        Firebase.auth.signOut()
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("723106455668-7apee9lsea93gpi66cjkoiom258i30e2.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .build()

        val signInClient = GoogleSignIn.getClient(context, signInOptions)
        signInClient.signOut()
    }

    fun createWorkspace(name: String, user: User) = viewModelScope.launch {
        if (user.workspaces.any { it.name == name }) {
            _message.value = "Workspace with the similar name is already created"
            return@launch
        }

        val workspace = Workspace(
            name = name,
            owner = FirestoreCollection.getReference(FirestoreCollection.USERS, user.uid),
            members = listOf(WorkspaceMember(user.uid, WorkspaceRole.ADMIN)),
        )

        when (val result = firestoreRepository.addWorkspace(user, workspace)) {
            is Result.Success -> _message.value = ToastMessage.WORKSPACE_CREATED
            is Result.Error -> _message.value = result.message
            is Result.Exception -> _message.value = result.message
        }
    }

    fun selectWorkspace(workspaceId: String?) = viewModelScope.launch {
        when (val result = firestoreRepository.getWorkspace(workspaceId)) {
            is Result.Success -> _currentWorkspace.value = result.data
            is Result.Error -> _message.value = result.message
            is Result.Exception -> _message.value = result.message
        }
    }
}