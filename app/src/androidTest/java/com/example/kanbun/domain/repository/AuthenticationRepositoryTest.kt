package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.data.repository.AuthenticationRepositoryImpl
import com.example.kanbun.domain.FirestoreTestUtil
import com.example.kanbun.isResultError
import com.example.kanbun.isResultSuccess
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthenticationRepositoryTest {
    private lateinit var authRepository: AuthenticationRepositoryImpl

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        val auth = FirestoreTestUtil.auth
        val dispatcher = UnconfinedTestDispatcher()
        authRepository = AuthenticationRepositoryImpl(auth, dispatcher)
    }

    @After
    fun tearDown() = runBlocking {
        FirestoreTestUtil.deleteAuthData()
    }

    @Test
    fun signUpWithEmail_signsUpUserWithCredentials(): Unit = runBlocking {
        val name = FirestoreTestUtil.testName
        val email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword
        val result = authRepository.signUpWithEmail(name, email, password)

        assertThat(result).isResultSuccess()

        val resultData = (result as Result.Success).data

        assertThat(resultData.displayName).isEqualTo(name)
        assertThat(resultData.email).isEqualTo(email)
    }

    @Test
    fun signUpWithEmail_returnsResultException_ifUserIsAlreadyRegistered() = runBlocking {
        val name = FirestoreTestUtil.testName
        val email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword
        authRepository.signUpWithEmail(name, email, password)
        val result = authRepository.signUpWithEmail(name, email, password)

        assertThat(result).isResultError()

        val resultData = (result as Result.Error)

        assertThat(resultData.message).isNotNull()
        assertThat(resultData.message).isNotEmpty()
    }

    @Test
    fun signInWithEmail_signsInUserWithEmailCredentials() = runBlocking {
        val name = FirestoreTestUtil.testName
        val email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword
        authRepository.signUpWithEmail(name, email, password)
        val result = authRepository.signInWithEmail(email, password)

        assertThat(result).isResultSuccess()

        val resultData = (result as Result.Success).data

        assertThat(resultData.displayName).isEqualTo(name)
        assertThat(resultData.email).isEqualTo(email)
    }

    @Test
    fun signInWithEmail_unregisteredUser_returnResultError() = runBlocking {
        val email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword
        val result = authRepository.signInWithEmail(email, password)

        assertThat(result).isResultError()

        val resultData = (result as Result.Error)

        assertThat(resultData.message).isNotNull()
        assertThat(resultData.message).isNotEmpty()
    }

    @Test
    fun sendVerificationEmail_successfullySendsVerificationEmail() = runBlocking {
        val name = FirestoreTestUtil.testName
        val email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword
        authRepository.signUpWithEmail(name, email, password)
        val result = authRepository.sendVerificationEmail(Firebase.auth.currentUser)

        assertThat(result).isResultSuccess()
    }
}