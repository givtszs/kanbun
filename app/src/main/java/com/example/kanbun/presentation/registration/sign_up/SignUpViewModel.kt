package com.example.kanbun.presentation.registration.sign_up

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.AuthType
import com.example.kanbun.common.Result
import com.example.kanbun.domain.usecase.RegisterUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase
) : ViewModel() {
    private var _signUpState = MutableStateFlow(SignUpViewState())
    val signUpState: StateFlow<SignUpViewState> = _signUpState

    var userEmail: String = signUpState.value.userEmail
        set(value) {
            field = value
            _signUpState.update { it.copy(userEmail = field) }
        }

    fun registerUser(
        email: String,
        password: String,
        confirmationPassword: String,
        provider: AuthType,
        successCallback: () -> Unit
    ) = viewModelScope.launch {
        if (confirmationPassword != password) {
            _signUpState.update { it.copy(confirmationPasswordError = "Passwords don't match. Please try again") }
            return@launch
        }

        when (provider) {
            AuthType.EMAIL -> {
                when (val result = registerUserUseCase.registerWithEmail(email, password)) {
                    is Result.Success -> {
                        _signUpState.update { it.copy(message = "Signed up successfully!") }
                        successCallback()
                    }

                    is Result.Error -> {
                        processError(result.message)
                    }
                }
            }

            AuthType.GOOGLE -> {}
            AuthType.GITHUB -> {}
        }
    }

    private fun processError(message: String?) {
        if (message == null) {
            return
        }

        if (message.lowercase().contains("email")) {
            _signUpState.update { it.copy(emailError = message) }
        } else if (message.lowercase().contains("password")) {
            _signUpState.update { it.copy(passwordError = message) }
        } else {
            _signUpState.update { it.copy(message = "Unknown error") }
        }
    }
}