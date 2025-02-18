package com.example.kanbun.ui.registration

import android.app.Activity
import android.content.Context
import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.BuildConfig
import com.example.kanbun.R
import com.example.kanbun.common.AuthProvider
import com.example.kanbun.common.Result
import com.example.kanbun.common.ToastMessage
import com.example.kanbun.domain.repository.AuthenticationRepository
import com.example.kanbun.domain.usecase.ManageFirestoreUserUseCase
import com.example.kanbun.domain.utils.ConnectivityChecker
import com.example.kanbun.ui.ViewState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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
    private val authRepository: AuthenticationRepository,
    private val manageFirestoreUserUseCase: ManageFirestoreUserUseCase,
    private val connectivityChecker: ConnectivityChecker
) : ViewModel() {
    protected val _authState = MutableStateFlow(ViewState.AuthState())

    /**
     * Resets the error state for a text field identified by the provided [layoutId]
     * @param layoutId the id of the text field.
     */
    fun resetTextFieldError(@IdRes layoutId: Int) {
        when (layoutId) {
            R.id.tfName -> _authState.update { it.copy(nameError = null) }
            R.id.tfEmail -> _authState.update { it.copy(emailError = "") }
            R.id.tfPassword -> _authState.update { it.copy(passwordError = "") }
            R.id.tfConfirmPassword -> _authState.update { it.copy(confirmationPasswordError = null) }
        }
    }

    protected fun notifyNoInternet() {
        showMessage(ToastMessage.NO_NETWORK_CONNECTION)
    }

    /**
     * Processes authentication errors and updates the view state accordingly.
     * @param message the error message received from the authentication process.
     */
    protected fun processAuthenticationError(message: String?) {
        if (message == null) {
            return
        }

        val messageLowercase = message.lowercase()
        when {
            messageLowercase.contains("name") -> _authState.update { it.copy(nameError = message) }
            messageLowercase.contains("email") ->  _authState.update { it.copy(emailError = message) }
            messageLowercase.contains("password") ->  _authState.update { it.copy(passwordError = message) }
            else -> _authState.update { it.copy(message = message) }
        }
    }

    fun showMessage(message: String?) {
        _authState.update { it.copy(message = message) }
    }

    /**
     * Resets the UI message state, indicating that the message has been displayed.
     */
    fun messageShown() {
        _authState.update { it.copy(message = null) }
    }

    suspend fun saveUserData(user: FirebaseUser, provider: AuthProvider) {
        val result = manageFirestoreUserUseCase.saveUser(user, provider)
        if (result is Result.Error) {
            showMessage(result.message)
        }
    }

    /**
     * Retrieves the configured GoogleSignInClient for authentication with Google.
     * @param context application context.
     * @return configured [GoogleSignInClient].
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        if (!connectivityChecker.hasInternetConnection()) {
            notifyNoInternet()
        }

        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.serverClientId)
            .requestEmail()
            .requestProfile()
            .build()

        return GoogleSignIn.getClient(context, signInOptions)
    }

    /**
     * Initiates Google sign in authentication and handles the result.
     * @param accountId see [GoogleSignInAccount.getIdToken].
     * @param successCallback callback executed upon successful authentication.
     */
    fun authWithGoogle(accountId: String?, successCallback: suspend (FirebaseUser) -> Unit) =
        viewModelScope.launch {
            if (!connectivityChecker.hasInternetConnection()) {
                notifyNoInternet()
                return@launch
            }

            when (val result = authRepository.authWithGoogle(accountId)) {
                is Result.Success -> {
                    successCallback(result.data)
                }

                is Result.Error -> showMessage(result.message)
            }
        }

    /**
     * Initiates GitHub authentication and handles the result.
     * @param activity host activity.
     * @param successCallback callback executed upon successful authentication.
     */
    fun authWithGitHub(activity: Activity, successCallback: suspend (FirebaseUser) -> Unit) =
        viewModelScope.launch {
            if (!connectivityChecker.hasInternetConnection()) {
                notifyNoInternet()
            }

            when (val result = authRepository.authWithGitHub(activity)) {
                is Result.Success -> {
                    successCallback(result.data)
                }

                is Result.Error -> showMessage(result.message)
            }
        }
}