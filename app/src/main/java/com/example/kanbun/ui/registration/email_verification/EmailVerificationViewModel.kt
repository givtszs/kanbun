package com.example.kanbun.ui.registration.email_verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.AuthProvider
import com.example.kanbun.common.EMAIL_RESEND_TIME_LIMIT
import com.example.kanbun.common.Result
import com.example.kanbun.common.ToastMessage
import com.example.kanbun.domain.usecase.RegisterUserUseCase
import com.example.kanbun.domain.utils.ConnectivityChecker
import com.example.kanbun.ui.ViewState
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase,
    private val connectivityChecker: ConnectivityChecker
) : ViewModel() {
    private val _emailVerificationState = MutableStateFlow(ViewState.EmailVerificationState())
    val emailVerificationState: StateFlow<ViewState.EmailVerificationState> =
        _emailVerificationState
    var user = Firebase.auth.currentUser

    init {
        if (user == null) {
            throw IllegalArgumentException("User must be registered to access this screen")
        }

        sendVerificationEmail(resend = false)
    }

    fun getUserInfo(): UserInfo? {
        return user?.providerData?.first { it.providerId == AuthProvider.EMAIL.providerId}
    }

    fun messageShown() {
        _emailVerificationState.update { it.copy(message = null) }
    }

    private fun showMessage(message: String?) {
        _emailVerificationState.update { it.copy(message = message) }
    }

    /**
     * Sends a verification email to the user.
     *
     * If the [resend] is `true`, starts countdown for the next resend availability.
     * @param resend flag indicating whether to resend the verification email.
     */
    fun sendVerificationEmail(resend: Boolean) = viewModelScope.launch {
        if (!connectivityChecker.hasInternetConnection()) {
            _emailVerificationState.update { it.copy(message = ToastMessage.NO_NETWORK_CONNECTION) }
            return@launch
        }

        when (val result = registerUserUseCase.sendVerificationEmail(user)) {
            is Result.Success -> showMessage("Verification email has been sent successfully")
            is Result.Error -> showMessage(result.message)
            is Result.Exception -> showMessage(result.message)
        }

        if (resend) {
            startCountdown()
            _emailVerificationState.update { it.copy(isResendAvailable = false) }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            var time = EMAIL_RESEND_TIME_LIMIT
            while (time > 0) {
                _emailVerificationState.update { it.copy(countdownMillis = time) }
                delay(1000L)
                --time
            }
            _emailVerificationState.update { it.copy(isResendAvailable = true) }
        }
    }

    /**
     * Updates cached [FirebaseUser] state.
     */
    suspend fun updateUser() {
        user?.reload()?.await()
    }
}