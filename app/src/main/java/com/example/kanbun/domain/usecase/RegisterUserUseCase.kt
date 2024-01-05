package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class RegisterUserUseCase @Inject constructor(
    private val auth: FirebaseAuth
) {
    suspend fun registerWithEmail(email: String, password: String): Result<Unit> {
        throw IllegalAccessException("Not yet implemented")
    }
}