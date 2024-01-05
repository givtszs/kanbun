package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.FirestoreTestUtil
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class RegisterUserUseCaseTest {
    private lateinit var useCase: RegisterUserUseCase

    @Before
    fun setUp() {
        val auth = FirestoreTestUtil.auth
        useCase = RegisterUserUseCase(auth)
    }

    @Test
    fun registerWithEmail_registersUserWithEmailCredentials(): Unit = runBlocking {
        val email = "test.email@gmail.com"
        val password = "qwerty123_"

        val result = useCase.registerWithEmail(email, password)

        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

}