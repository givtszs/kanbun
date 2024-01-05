package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.FirestoreTestUtil
import com.google.common.truth.Subject
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class RegisterUserUseCaseTest {
    private lateinit var useCase: RegisterUserUseCase

    @Before
    fun setUp() {
        val auth = FirestoreTestUtil.auth
        useCase = RegisterUserUseCase(auth)
    }

    @After
    fun tearDown() = runBlocking {
        FirestoreTestUtil.deleteAuthData()
    }

    @Test
    fun registerWithEmail_registersUserWithEmailCredentials(): Unit = runBlocking {
        val email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword

        val result = useCase.registerWithEmail(email, password)
        assertThat(result).isInstanceOf(Result.Success::class.java)

        val user = (result as Result.Success).data
        assertThat(user.email).isEqualTo(email)
    }

    private fun assertThatResultErrorWithPresentMessage(result: Result<Any>) {
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error)
        assertThat(error.message).isNotNull()
        assertThat(error.message).isNotEmpty()
    }

    @Test
    fun registerWithEmail_returnsResultError_ifUserIsAlreadyRegistered() = runBlocking {
        val email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword

        val result = useCase.registerWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)
    }

    @Test
    fun registerUser_withInvalidEmail_returnsResultError() = runBlocking {
        var email = ""
        val password = FirestoreTestUtil.testPassword

        var result = useCase.registerWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        email = "q"
        result = useCase.registerWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        email = "qatesteverything@"
        result = useCase.registerWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        email = "qatesteverything..@gmail.com"
        result = useCase.registerWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)


        email = ".qatesteverything@gmail.com"
        result = useCase.registerWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        email = "qatesteverything@@gmail.com"
        result = useCase.registerWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)
    }

    @Test
    fun registerUser_withInvalidPassword_returnsResultError() = runBlocking {
        val email = FirestoreTestUtil.testEmail
        var password = ""

        var result = useCase.registerWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "123"
        result = useCase.registerWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)
    }
}