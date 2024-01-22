package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.FirestoreTestUtil
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
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
    fun signUpWithEmail_signsUpUserWithEmailCredentials(): Unit = runBlocking {
        val name = FirestoreTestUtil.testName
        val email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword

        val result = useCase.signUpWithEmail(name, email, password)
        assertThat(result).isInstanceOf(Result.Success::class.java)

        val user = (result as Result.Success).data
        assertThat(user.displayName).isEqualTo(name)
        assertThat(user.email).isEqualTo(email)
    }

    private fun assertThatResultErrorWithPresentMessage(result: Result<Any>) {
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error)
        assertThat(error.message).isNotNull()
        assertThat(error.message).isNotEmpty()
    }

    @Test
    fun signUpWithEmail_returnsResultException_ifUserIsAlreadyRegistered() = runBlocking {
        val name = FirestoreTestUtil.testName
        val email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword

        useCase.signUpWithEmail(name, email, password)

        val result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)
    }

    @Test
    fun signUpUser_withInvalidName_returnsResultError() = runBlocking {
        var name = "FirestoreTestUtil.testName"
        val email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword

        var result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        name = "q"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        name = "12314"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        name = "Test12"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)


        name = "Test*"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        name = "Johnathan Michael Alexander William Benjamin Theodore Christopher David Edward Richard Anthony Joseph Charles Frederick Daniel Matthew Patrick Robert George Henry Samuel Jackson"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)
    }


    @Test
    fun signUpUser_withInvalidEmail_returnsResultError() = runBlocking {
        val name = FirestoreTestUtil.testName
        var email = ""
        val password = FirestoreTestUtil.testPassword

        var result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        email = "q"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        email = "qatesteverything@"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        email = "qatesteverything..@gmail.com"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)


        email = ".qatesteverything@gmail.com"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        email = "qatesteverything@@gmail.com"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)
    }

    @Test
    fun signUpUser_withInvalidPassword_returnsResultError() = runBlocking {
        val name = FirestoreTestUtil.testName
        val email = FirestoreTestUtil.testEmail
        var password = ""

        var result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "123"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "111111"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "qw1Erty y"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "qwerty123"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "QWERTY123"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "qqqqqq"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "TestPassword123!ThisIsALongPasswordForTesting1234567890123456789012345678901"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "Qwerty123"
        result = useCase.signUpWithEmail(name, email, password)
        assertThatResultErrorWithPresentMessage(result)
    }

    @Test
    fun signInWithEmail_signsInUserWithEmailCredentials() = runBlocking {
        val name = FirestoreTestUtil.testName
        val email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword

        useCase.signUpWithEmail(name, email, password)

        val result = useCase.signInWithEmail(email, password)
        assertThat(result).isInstanceOf(Result.Success::class.java)

        val user = (result as Result.Success).data
        assertThat(user.displayName).isEqualTo(name)
        assertThat(user.email).isEqualTo(email)
    }

    @Test
    fun signInWithEmail_returnResultException_whenSignInRegisteredUser() = runBlocking {
        val email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword

        val result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)
    }

    @Test
    fun signInUser_withInvalidEmail_returnsResultError() = runBlocking {
        val name = FirestoreTestUtil.testName
        var email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword

        useCase.signUpWithEmail(name, email, password)

        email = ""
        var result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        email = "q"
        result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        email = "qatesteverything@"
        result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        email = "qatesteverything..@gmail.com"
        result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)


        email = ".qatesteverything@gmail.com"
        result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        email = "qatesteverything@@gmail.com"
        result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)
    }

    @Test
    fun signInUser_withInvalidPassword_returnsResultError() = runBlocking {
        val name = FirestoreTestUtil.testName
        val email = FirestoreTestUtil.testEmail
        var password = FirestoreTestUtil.testPassword

        useCase.signUpWithEmail(name, email, password)

        password = ""
        var result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "123"
        result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "111111"
        result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "qw1Erty y"
        result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "qwerty123"
        result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "QWERTY123"
        result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "qqqqqq"
        result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "TestPassword123!ThisIsALongPasswordForTesting1234567890123456789012345678901"
        result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)

        password = "Qwerty123"
        result = useCase.signInWithEmail(email, password)
        assertThatResultErrorWithPresentMessage(result)
    }

    @Test
    fun sendVerificationEmail_successfullySendsVerificationEmail() = runBlocking {
        val name = FirestoreTestUtil.testName
        val email = FirestoreTestUtil.testEmail
        val password = FirestoreTestUtil.testPassword

        useCase.signUpWithEmail(name, email, password)
        val result = useCase.sendVerificationEmail(Firebase.auth.currentUser)
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }
}