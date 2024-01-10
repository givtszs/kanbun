package com.example.kanbun.presentation.registration.sign_up

import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.AuthType
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

    fun signUpUser(
        name: String,
        email: String,
        password: String,
        confirmationPassword: String,
        provider: AuthType,
        successCallback: () -> Unit
    ) = viewModelScope.launch {
        if (confirmationPassword != password) {
            _authState.update { it.copy(confirmationPasswordError = "Passwords don't match. Please try again") }
            return@launch
        }

        when (provider) {
            AuthType.EMAIL -> {
                when (val result = registerUserUseCase.signUpWithEmail(name, email, password)) {
                    is Result.Success -> {
                        _authState.update { it.copy(message = "Signed up successfully!") }
                        successCallback()
                    }

                    is Result.Error -> processError(result.message)
                    is Result.Exception -> _authState.update { it.copy(message = result.message) }
                }
            }

            AuthType.GOOGLE -> {}
            AuthType.GITHUB -> {}
        }
    }
}