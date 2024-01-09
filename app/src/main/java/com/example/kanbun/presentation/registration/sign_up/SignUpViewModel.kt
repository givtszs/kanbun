package com.example.kanbun.presentation.registration.sign_up

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.AuthType
import com.example.kanbun.common.Result
import com.example.kanbun.domain.usecase.RegisterUserUseCase
import com.example.kanbun.presentation.ViewState
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
    private var _signUpState = MutableStateFlow(ViewState.SignUpViewState())
    val signUpState: StateFlow<ViewState.SignUpViewState> = _signUpState

    var emailError: String = ""
        set(value) {
            field = value
            _signUpState.update { it.copy(emailError = field) }
        }

    var passwordError: String = ""
        set(value) {
            field = value
            _signUpState.update { it.copy(passwordError = field) }
        }

    var confirmationPasswordError: String? = null
        set(value) {
            field = value
            _signUpState.update { it.copy(confirmationPasswordError = field) }
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
                when (val result = registerUserUseCase.signUpWithEmail(email, password)) {
                    is Result.Success -> {
                        _signUpState.update { it.copy(message = "Signed up successfully!") }
                        successCallback()
                    }

                    is Result.Error -> processError(result.message)
                    is Result.Exception -> _signUpState.update { it.copy(message = result.message) }
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
        }

        if (message.lowercase().contains("password")) {
            _signUpState.update { it.copy(passwordError = message) }
        }
    }

    fun messageShown() {
        _signUpState.update { it.copy(message = null) }
    }
}