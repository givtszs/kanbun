package com.example.kanbun.presentation.registration.email_verification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.EMAIL_RESEND_TIME_LIMIT
import com.example.kanbun.domain.usecase.RegisterUserUseCase
import com.example.kanbun.presentation.ViewState
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase
) : ViewModel() {
    private val _emailVerificationState = MutableStateFlow(ViewState.EmailVerificationState())
    val emailVerificationState: StateFlow<ViewState.EmailVerificationState> = _emailVerificationState
    val user = Firebase.auth.currentUser

    init {
        if (user == null) {
            throw IllegalArgumentException("User must be registered to access this screen")
        }

        sendVerificationEmail(resend = false)
    }

    fun sendVerificationEmail(resend: Boolean) = viewModelScope.launch {
        registerUserUseCase.sendVerificationEmail(user)

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
}