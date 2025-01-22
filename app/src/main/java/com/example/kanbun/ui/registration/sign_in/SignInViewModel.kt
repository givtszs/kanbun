package com.example.kanbun.ui.registration.sign_in

import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.repository.AuthenticationRepository
import com.example.kanbun.domain.usecase.ManageFirestoreUserUseCase
import com.example.kanbun.domain.usecase.SignInWithEmailUseCase
import com.example.kanbun.domain.utils.ConnectivityChecker
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.registration.AuthViewModel
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val connectivityChecker: ConnectivityChecker,
    manageFirestoreUserUseCase: ManageFirestoreUserUseCase,
    authRepository: AuthenticationRepository
) : AuthViewModel(authRepository, manageFirestoreUserUseCase, connectivityChecker) {
    val signInState: StateFlow<ViewState.AuthState> = _authState

    /**
     * Initiates the sign in process with the provided [email] and [password], and handles the result.
     * @param email user's email address
     * @param password user's password
     * @param successCallback callback executed upon successful sign in
     */
    fun signInWithEmail(
        email: String,
        password: String,
        successCallback: (FirebaseUser) -> Unit
    ) = viewModelScope.launch {
        if (!connectivityChecker.hasInternetConnection()) {
            notifyNoInternet()
            return@launch
        }

        when (val result = signInWithEmailUseCase(email, password)) {
            is Result.Success -> successCallback(result.data)
            is Result.Error -> processAuthenticationError(result.message)
        }
    }
}