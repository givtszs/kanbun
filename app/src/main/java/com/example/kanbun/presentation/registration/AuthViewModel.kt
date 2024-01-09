package com.example.kanbun.presentation.registration

import androidx.lifecycle.ViewModel
import com.example.kanbun.presentation.ViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

open class AuthViewModel : ViewModel() {
    protected val _authState = MutableStateFlow(ViewState.AuthState())
    
    fun resetEmailError() {
        _authState.update { it.copy(emailError = "") }
    }
    
    fun resetPasswordError() {
        _authState.update { it.copy(passwordError = "") }
    }

    fun resetConfirmationPasswordError() {
        _authState.update { it.copy(confirmationPasswordError = null) }
    }
    
    protected fun processError(message: String?) {
        if (message == null) {
            return
        }

        if (message.lowercase().contains("email")) {
            _authState.update { it.copy(emailError = message) }
        }

        if (message.lowercase().contains("password")) {
            _authState.update { it.copy(passwordError = message) }
        }
    }

    fun messageShown() {
        _authState.update { it.copy(message = null) }
    }
}