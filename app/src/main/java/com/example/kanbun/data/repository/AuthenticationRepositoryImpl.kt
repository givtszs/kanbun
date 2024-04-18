package com.example.kanbun.data.repository

import android.app.Activity
import android.util.Log
import com.example.kanbun.common.Result
import com.example.kanbun.common.runCatching
import com.example.kanbun.di.IoDispatcher
import com.example.kanbun.domain.repository.AuthenticationRepository
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthenticationRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthenticationRepository {

    companion object {
        private const val TAG = "AuthenticationRepository"
    }

    override suspend fun signUpWithEmail(
        name: String,
        email: String,
        password: String
    ): Result<FirebaseUser> = runCatching {
        withContext(ioDispatcher) {
            val user = auth.createUserWithEmailAndPassword(email, password).await().user
            if (user != null) {
                user.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                ).await()
                Log.d(TAG, "User operation successful: ${user.email}")
                user
            } else {
                Log.d(TAG, "Operation failed: Couldn't retrieve user information")
                throw RuntimeException("Couldn't sign up a user with email and password")
            }
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        withContext(ioDispatcher) {
            val user = auth.signInWithEmailAndPassword(email, password).await().user
            if (user != null) {
                Log.d(TAG, "User operation successful: ${user.email}")
                user
            } else {
                Log.d(TAG, "Operation failed: Couldn't retrieve user information")
                throw IllegalStateException("Couldn't retrieve user information")
            }
        }
    }

    override suspend fun sendVerificationEmail(user: FirebaseUser?): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            user?.sendEmailVerification()?.await()
        }
    }

    override suspend fun authWithGoogle(accountId: String?): Result<FirebaseUser> = runCatching {
        withContext(ioDispatcher) {
            val credentials = GoogleAuthProvider.getCredential(accountId, null)
            val user = auth.signInWithCredential(credentials).await().user
            if (user != null) {
                Log.d(TAG, "Signed in with Google successfully")
                user
            } else {
                Log.d(TAG, "Operation failed: Couldn't retrieve user information")
                throw RuntimeException("Couldn't authenticate user via Google")
            }
        }
    }

    override suspend fun authWithGitHub(activity: Activity): Result<FirebaseUser> = runCatching {
        val provider = OAuthProvider.newBuilder("github.com").build()
        withContext(ioDispatcher) {
            Firebase.auth.startActivityForSignInWithProvider(activity, provider)
                .await<AuthResult?>()
                ?.user ?: throw RuntimeException("Couldn't authenticate user via GitHub")
        }
    }
}