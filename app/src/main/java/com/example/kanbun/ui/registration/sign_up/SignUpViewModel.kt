package com.example.kanbun.ui.registration.sign_up

import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.AuthProvider
import com.example.kanbun.common.Result
import com.example.kanbun.domain.repository.AuthenticationRepository
import com.example.kanbun.domain.usecase.ManageFirestoreUserUseCase
import com.example.kanbun.domain.usecase.SignUpWithEmailUseCase
import com.example.kanbun.domain.utils.ConnectivityChecker
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.registration.AuthViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    private val manageFirestoreUserUseCase: ManageFirestoreUserUseCase,
    private val connectivityChecker: ConnectivityChecker,
    authRepository: AuthenticationRepository
) : AuthViewModel(authRepository, manageFirestoreUserUseCase, connectivityChecker) {
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
        if (!connectivityChecker.hasInternetConnection()) {
            notifyNoInternet()
            return@launch
        }

        if (confirmationPassword != password) {
            _authState.update { it.copy(confirmationPasswordError = "Passwords don't match. Please try again") }
            return@launch
        }

        when (val result = signUpWithEmailUseCase(name, email, password)) {
            is Result.Success -> {
                manageFirestoreUserUseCase.saveUser(result.data, AuthProvider.EMAIL)
                successCallback()
            }

            is Result.Error -> processAuthenticationError(result.message)
        }
    }
}