package com.example.kanbun.presentation.registration.sign_in

import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.usecase.RegisterUserUseCase
import com.example.kanbun.presentation.ViewState
import com.example.kanbun.presentation.registration.AuthViewModel
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase
) : AuthViewModel(registerUserUseCase) {
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
        when (val result = registerUserUseCase.signInWithEmail(email, password)) {
            is Result.Success -> {
                _authState.update { it.copy(message = "Signed up successfully!") }
                successCallback(result.data)
            }

            is Result.Error -> processAuthenticationError(result.message)
            is Result.Exception -> _authState.update { it.copy(message = result.message) }
        }
    }
}