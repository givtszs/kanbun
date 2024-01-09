package com.example.kanbun.presentation.registration.sign_in

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
class SignInViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase
): AuthViewModel() {
    val signInState: StateFlow<ViewState.AuthState> = _authState
    
    fun signInUser(
        email: String,
        password: String,
        provider: AuthType,
        successCallback: () -> Unit
    ) = viewModelScope.launch {
        when (provider) {
            AuthType.EMAIL -> {
                when (val result = registerUserUseCase.signInWithEmail(email, password)) {
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