package com.example.kanbun.presentation.root.user_boards

import android.content.Context
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
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class UserBoardsViewModel @Inject constructor(
    private val connectivityChecker: ConnectivityChecker,
    private val firestoreRepository: FirestoreRepository
) : BaseViewModel() {
    private var _userBoardsState = MutableStateFlow(ViewState.UserBoardsViewState())
    val userBoardsState: StateFlow<ViewState.UserBoardsViewState> = _userBoardsState

    suspend fun updateUser(){
        if (!connectivityChecker.hasInternetConnection()) {
            _userBoardsState.update { it.copy(messanger = it.messanger.copy(message = ToastMessage.NO_NETWORK_CONNECTION)) }
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