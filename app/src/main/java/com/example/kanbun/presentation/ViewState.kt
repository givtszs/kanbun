package com.example.kanbun.presentation

import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

sealed class ViewState {
    data class AuthState(
        val nameError: String? = null,
        val emailError: String = "",
        val passwordError: String = "",
        val confirmationPasswordError: String? = null,
        val message: String? = null
    ) : ViewState() {
        /**
         * Handles the text field error state
          */
        fun processError(error: String?, textField: TextInputLayout) {
            if (!error.isNullOrEmpty()) {
                textField.apply {
                    setError(error)
                    isErrorEnabled = true
                }
            }
        }
    }

    data class EmailVerificationState(
        val isResendAvailable: Boolean = true,
        val countdownMillis: Int = 0,
        val message: String? = null
    ) : ViewState()
}
