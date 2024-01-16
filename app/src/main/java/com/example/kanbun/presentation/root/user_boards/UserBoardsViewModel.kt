package com.example.kanbun.presentation.root.user_boards

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.common.ToastMessage
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "UserBoardsViewModel"

@HiltViewModel
class UserBoardsViewModel @Inject constructor(
    private val connectivityChecker: ConnectivityChecker,
    private val firestoreRepository: FirestoreRepository
) : BaseViewModel() {
    private var _userBoardsState = MutableStateFlow(ViewState.UserBoardsViewState())
    val userBoardsState: StateFlow<ViewState.UserBoardsViewState> = _userBoardsState

    init {
        getUser()
    }

    private fun getUser() = viewModelScope.launch {
        if (!connectivityChecker.hasInternetConnection()) {
            Log.d(TAG, ToastMessage.NO_NETWORK_CONNECTION)
            _userBoardsState.update { it.copy(messanger = it.messanger.copy(ToastMessage.NO_NETWORK_CONNECTION)) }
        }

        when (val result = firestoreRepository.getUser(firebaseUser?.uid)) {
            is Result.Success -> {
                Log.d(TAG, "${result.data}")
                _userBoardsState.update { it.copy(user = result.data) }
            }

            is Result.Error -> _userBoardsState.update {
                it.copy(
                    messanger = it.messanger.showMessage(
                        result.message
                    )
                )
            }

            is Result.Exception -> _userBoardsState.update {
                it.copy(
                    messanger = it.messanger.showMessage(
                        result.message
                    )
                )
            }
        }
    }

    suspend fun updateUser() {
        if (!connectivityChecker.hasInternetConnection()) {
            _userBoardsState.update { it.copy(messanger = it.messanger.showMessage(ToastMessage.NO_NETWORK_CONNECTION)) }
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
}