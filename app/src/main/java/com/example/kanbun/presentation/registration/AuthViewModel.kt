package com.example.kanbun.presentation.registration

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.R
import com.example.kanbun.common.Result
import com.example.kanbun.domain.usecase.ManageFirestoreUserUseCase
import com.example.kanbun.domain.usecase.RegisterUserUseCase
import com.example.kanbun.presentation.ViewState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class AuthViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase
) : ViewModel() {
    protected val _authState = MutableStateFlow(ViewState.AuthState())

    @Inject
    lateinit var manageFirestoreUserUseCase: ManageFirestoreUserUseCase

    fun resetTextFieldError(@IdRes layoutId: Int) {
        Log.d("AuthViewModel", "resetError: layoutId: ${layoutId}")
        when (layoutId) {
            R.id.tfName -> _authState.update { it.copy(nameError = null) }
            R.id.tfEmail -> _authState.update { it.copy(emailError = "") }
            R.id.tfPassword -> _authState.update { it.copy(emailError = "") }
            R.id.tfConfirmPassword -> _authState.update { it.copy(confirmationPasswordError = null) }
        }
    }
    
    protected fun processAuthenticationError(message: String?) {
        if (message == null) {
            return
        }

        if (message.lowercase().contains("name")) {
            _authState.update { it.copy(nameError = message) }
            return
        }

        if (message.lowercase().contains("email")) {
            _authState.update { it.copy(emailError = message) }
            return
        }

        if (message.lowercase().contains("password")) {
            _authState.update { it.copy(passwordError = message) }
            return
        }
    }

    fun messageShown() {
        _authState.update { it.copy(message = null) }
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("723106455668-7apee9lsea93gpi66cjkoiom258i30e2.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .build()

        Log.d("SignUpFragment", "signInOptions: ${signInOptions.account}")

        return GoogleSignIn.getClient(context, signInOptions)
    }

    fun authWithGoogle(accountId: String?, successCallback: (FirebaseUser) -> Unit) = viewModelScope.launch {
        when (val result = registerUserUseCase.authWithGoogle(accountId)) {
            is Result.Success -> {
                manageFirestoreUserUseCase.saveUser(result.data)
                successCallback(result.data)
            }

            is Result.Error -> _authState.update { it.copy(message = result.message) }

            is Result.Exception -> _authState.update { it.copy(message = result.message) }
        }
    }

    fun authWithGitHub(activity: Activity, successCallback: (FirebaseUser) -> Unit) = viewModelScope.launch {
        when (val result = registerUserUseCase.authWithGitHub(activity)) {
            is Result.Success -> successCallback(result.data)

            is Result.Error -> _authState.update { it.copy(message = result.message) }

            is Result.Exception -> _authState.update { it.copy(message = result.message) }
        }
    }
}