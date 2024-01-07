package com.example.kanbun.presentation.registration.sign_up

data class SignUpViewState(
    val userEmail: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmationPasswordError: String? = null,
    val message: String? = null
)