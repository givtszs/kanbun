package com.example.kanbun.presentation.registration.sign_up

import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.usecase.RegisterUserUseCase
import com.example.kanbun.presentation.ViewState
import com.example.kanbun.presentation.registration.AuthViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase
) : AuthViewModel(registerUserUseCase) {
    val signUpState: StateFlow<ViewState.AuthState> = _authState

    /**
     * Initiates the sign up process with the provided user credentials and handles the result.
     * @param name user's name
     * @param email user's email address
     * @param password user's password
     * @param confirmationPassword
     * @param successCallback callback executed upon successful sign in
     */
    fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        confirmationPassword: String,
        successCallback: () -> Unit
    ) = viewModelScope.launch {
        if (confirmationPassword != password) {
            _authState.update { it.copy(confirmationPasswordError = "Passwords don't match. Please try again") }
            return@launch
        }

        when (val result = registerUserUseCase.signUpWithEmail(name, email, password)) {
            is Result.Success -> {
                _authState.update { it.copy(message = "Signed up successfully!") }
                successCallback()
            }

            is Result.Error -> processAuthenticationError(result.message)
            is Result.Exception -> _authState.update { it.copy(message = result.message) }
        }
    }
}