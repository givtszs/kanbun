package com.example.kanbun.presentation

sealed class ViewState {
    data class AuthViewState(
        val emailError: String = "",
        val passwordError: String = "",
        val confirmationPasswordError: String? = null,
        val message: String? = null
    ) : ViewState()
}
