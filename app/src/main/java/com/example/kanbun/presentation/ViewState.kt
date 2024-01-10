package com.example.kanbun.presentation

sealed class ViewState {
    data class AuthState(
        val nameError: String? = null,
        val emailError: String = "",
        val passwordError: String = "",
        val confirmationPasswordError: String? = null,
        val message: String? = null
    ) : ViewState()

    data class EmailVerificationState(
        val isResendAvailable: Boolean = true,
        val countdownMillis: Int = 0
    ) : ViewState()
}
