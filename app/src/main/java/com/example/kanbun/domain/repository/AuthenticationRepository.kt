package com.example.kanbun.domain.repository

import android.app.Activity
import com.example.kanbun.common.Result
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser

interface AuthenticationRepository {

    /**
     * Signs up a user with the provided credentials: a name, email and password.
     * @param name the user's name.
     * @param email the user's email.
     * @param password the user's password.
     * @return [Result] of the operation containing the [FirebaseUser] on success, or [Result.Error]
     * with an error message on failure.
     */
    suspend fun signUpWithEmail(name: String, email: String, password: String): Result<FirebaseUser>

    /**
     * Signs in a user with the provided [email] and [password].
     * @param email user's email.
     * @param password user's password.
     * @return [Result] of the operation containing the [FirebaseUser] on success, or an error message on failure.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser>

    /**
     * Sends a verification email to the [user].
     * @param user registered unverified [FirebaseUser].
     * @return [Result] of the operation containing [Unit] on success, an error message on failure.
     */
    suspend fun sendVerificationEmail(user: FirebaseUser?): Result<Unit>

    /**
     * Performs Google authentication.
     * @param accountId see [GoogleSignInAccount.getIdToken]
     * @return [Result] of the operation containing the [FirebaseUser] on success, or an error message on failure.
     */
    suspend fun authWithGoogle(accountId: String?): Result<FirebaseUser>

    /**
     * Performs GitHub authentication.
     * @param activity host activity.
     * @return [Result] of the operation containing the [FirebaseUser] on success, or an error message on failure.
     */
    suspend fun authWithGitHub(activity: Activity): Result<FirebaseUser>
}