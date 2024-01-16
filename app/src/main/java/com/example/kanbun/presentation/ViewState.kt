package com.example.kanbun.presentation

import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.google.android.material.textfield.TextInputLayout

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

    data class UserBoardsViewState(
        val user: User? = null,
        val workspaces: List<Workspace> = emptyList(),
        val currentWorkspace: String? = null,
        val isLoading: Boolean = true,
        val messanger: Messanger = Messanger()
    ) : ViewState()
}
