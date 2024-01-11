package com.example.kanbun.domain.usecase

import android.app.Activity
import android.util.Log
import android.util.Patterns
import com.example.kanbun.common.Result
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private val TAG = "RegisterUserUseCase"

/**
 * Use case responsible for handling user registration and authentication operations.
 * @property auth [FirebaseAuth] instance for authentication.
 */
class RegisterUserUseCase @Inject constructor(
    private val auth: FirebaseAuth
) {
    /**
     * Signs up a user with the provided credentials.
     * @param name user's name.
     * @param email user's email.
     * @param password user's password.
     * @return [Result] of the operation containing the [FirebaseUser] on success, or an error message on failure.
     */
    suspend fun signUpWithEmail(name: String, email: String, password: String): Result<FirebaseUser> {
        val areCredentialsValid = validateUserCredentials(name, email, password)
        if (!areCredentialsValid.first) {
            return Result.Error(areCredentialsValid.second)
        }

        return try {
            val user = auth.createUserWithEmailAndPassword(email, password).await().user
            if (user != null) {
                user.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                ).await()
                Log.d(TAG, "User operation successful: ${user.email}")
                Result.Success(user)
            } else {
                Log.d(TAG, "Operation failed: Couldn't retrieve user information")
                Result.Error("Operation failed: Couldn't retrieve user information")
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
            Result.Exception(e.message, e)
        }
    }

    /**
     * Signs in a user with the provided [email] and [password].
     * @param email user's email.
     * @param password user's password.
     * @return [Result] of the operation containing the [FirebaseUser] on success, or an error message on failure.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        val isEmailAndPasswordValid = validateEmailAndPassword(email, password)
        if (!isEmailAndPasswordValid.first) {
            return Result.Error(isEmailAndPasswordValid.second)
        }

        return try {
            val user = auth.signInWithEmailAndPassword(email, password).await().user
            if (user != null) {
                Log.d(TAG, "User operation successful: ${user.email}")
                Result.Success(user)
            } else {
                Log.d(TAG, "Operation failed: Couldn't retrieve user information")
                Result.Error("Operation failed: Couldn't retrieve user information")
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
            Result.Exception(e.message, e)
        }
    }

    private fun validateUserCredentials(name: String, email: String, password: String): Pair<Boolean, String> {
        val nameValidationResult = validateName(name)
        return if (!nameValidationResult.first) {
            nameValidationResult
        } else {
            validateEmailAndPassword(email, password)
        }
    }

    private fun validateName(name: String): Pair<Boolean, String> {
        val nameRegex = Regex("[a-zA-Z ]+")
        return when {
            name.isEmpty() -> Pair(false, "Name is required")
            name.length < 2 -> Pair(false, "Name should be at least 2 characters long")
            name.length > 124 -> Pair(false, "Name should be 124 characters or less")
            !name.matches(nameRegex) -> Pair(false, "Name should contain only letters")
            else -> Pair(true, "Name is valid")
        }
    }

    private fun validateEmailAndPassword(email: String, password: String): Pair<Boolean, String> {
        val passwordRegex = Regex("[~`!@#\$%^&*()_\\-+={}\\[\\]|\\\\:;\"'<,>.?/]")
        return when {
            email.isEmpty() -> Pair(false, "Email cannot be empty")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> Pair(false, "Invalid email format")
            email.substringBefore("@").endsWith(".") ||
                    email.substringBefore("@").startsWith(".") ->
                Pair(false, "Email cannot start or end with `.`")

            password.isEmpty() -> Pair(false, "Password cannot be empty")
            password.length < 6 -> Pair(false, "Password must be at least 6 characters long")
            password.length > 64 -> Pair(false, "Password cannot be longer that 64 characters")
            password.any { it.isWhitespace() } -> Pair(
                false,
                "Password must not contain whitespaces"
            )

            password.none { it.isDigit() } -> Pair(false, "Password must contain at least 1 digit")
            password.none { it.isLetter() } -> Pair(
                false,
                "Password must contain at least 1 letter"
            )

            password.none { it.isUpperCase() } -> Pair(
                false,
                "Password must contain at least 1 uppercase letter"
            )

            password.none { it.isLowerCase() } -> Pair(
                false,
                "Password must contain at least 1 lowercase letter"
            )

            !passwordRegex.containsMatchIn(password) -> Pair(
                false,
                "Password must contain at least 1 special character: ~`!@#\$%^&*()_-+={}[]|\\:;\"'<,>.?/"
            )

            else -> Pair(true, "Email and password are valid") // Validation successful
        }
    }

    /**
     * Sends a verification email to the [user].
     * @param user registered unverified [FirebaseUser].
     * @return [Result] of the operation containing [Unit] on success, an error message on failure.
     */
    suspend fun sendVerificationEmail(user: FirebaseUser?): Result<Unit> {
        if (user == null) {
            return Result.Error("User is not registered")
        }

        return try {
            user.sendEmailVerification().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Exception(e.message, e)
        }
    }

    /**
     * Performs Google authentication.
     * @param accountId see [GoogleSignInAccount.getIdToken]
     * @return [Result] of the operation containing the [FirebaseUser] on success, or an error message on failure.
     */
    suspend fun authWithGoogle(accountId: String?): Result<FirebaseUser> {
        return try {
            val credentials = GoogleAuthProvider.getCredential(accountId, null)
            val user = auth.signInWithCredential(credentials).await().user
            if (user != null) {
                Log.d(TAG, "Signed in with Google successfully")
                Result.Success(user)
            } else {
                Log.d(TAG, "Operation failed: Couldn't retrieve user information")
                Result.Error("Operation failed: Couldn't retrieve user information")
            }
        } catch (e: Exception) {
            Result.Exception(e.message, e)
        }
    }

    /**
     * Performs GitHub authentication.
     * @param activity host activity.
     * @return [Result] of the operation containing the [FirebaseUser] on success, or an error message on failure.
     */
    suspend fun authWithGitHub(activity: Activity): Result<FirebaseUser> {
        val provider = OAuthProvider.newBuilder("github.com").build()
        return try {
            val user =
                Firebase.auth.startActivityForSignInWithProvider(activity, provider).await().user
            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error("Operation failed: Couldn't retrieve user information")
            }
        } catch (e: Exception) {
            Result.Exception(e.message, e)
        }
    }
}