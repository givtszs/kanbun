package com.example.kanbun.ui

import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
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
        val currentWorkspace: Workspace? = null,
        val isLoading: Boolean = true,
        val message: String? = null
    ) : ViewState()

    data class BoardViewState(
        val board: Board = Board(),
        val lists: List<BoardList> = emptyList(),
        val isLoading: Boolean = false,
        val message: String? = null
    ) : ViewState()
}
