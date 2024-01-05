package com.example.kanbun.domain.usecase

import android.util.Log
import android.util.Patterns
import com.example.kanbun.common.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private val TAG = "RegisterUserUseCase"

class RegisterUserUseCase @Inject constructor(
    private val auth: FirebaseAuth
) {
    suspend fun registerWithEmail(email: String, password: String): Result<FirebaseUser> {
        val isEmailAndPasswordValid = validateEmailAndPassword(email, password)
        if (!isEmailAndPasswordValid.first) {
            Log.d(TAG, "Email or password is invalid: ${isEmailAndPasswordValid.second}")
            return Result.Error(isEmailAndPasswordValid.second)
        }

        return try {
            val task = auth.createUserWithEmailAndPassword(email, password).await()
            val user = task.user
            if (user != null) {
                Log.d(TAG, "Registered user: ${user.email}")
                Result.Success(user)
            } else {
                Log.e(TAG, "Couldn't register a user")
                Result.Error("Couldn't register a user")
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
            Result.Error(e.message, e)
        }
    }

    private fun validateEmailAndPassword(email: String, password: String): Pair<Boolean, String> {
        return when {
            email.isEmpty() -> Pair(false, "Email cannot be empty")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> Pair(false, "Invalid email format")
            email.substringBefore("@").endsWith(".") ||
                    email.substringBefore("@").startsWith(".") ->
                Pair(false, "Email cannot start or end with `.`")
            password.isEmpty() -> Pair(false, "Password cannot be empty")
            password.length < 6 -> Pair(false, "Password should be at least 6 characters long")
            password.none { it.isWhitespace() } -> Pair(false, "Password must not contain whitespaces")
            !password.any { it.isDigit() } -> Pair(false, "Password should contain at least 1 digit")
            // TODO("Add more checks")
            else -> Pair(true, "Email and password are valid") // Validation successful
        }
    }
}