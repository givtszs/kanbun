package com.example.kanbun.domain.usecase

import com.example.kanbun.common.AuthProvider
import com.example.kanbun.common.Result
import com.example.kanbun.data.repository.FirestoreRepositoryImpl
import com.example.kanbun.domain.FirestoreTestUtil
import com.example.kanbun.domain.repository.FirestoreRepository
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.IllegalStateException

class ManageFirestoreUserUseCaseTest {
    private lateinit var manageFirestoreUserUseCase: ManageFirestoreUserUseCase
    private lateinit var registerUserUseCase: RegisterUserUseCase
    private lateinit var firestoreRepository: FirestoreRepository
    private val auth = FirestoreTestUtil.auth
    private lateinit var user: FirebaseUser

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        registerUserUseCase = RegisterUserUseCase(auth)

        val firestore = FirestoreTestUtil.firestore
        val dispatcher = UnconfinedTestDispatcher()
        firestoreRepository = FirestoreRepositoryImpl(firestore, dispatcher)
        manageFirestoreUserUseCase = ManageFirestoreUserUseCase(firestoreRepository)

        runBlocking {
            val regResult = registerUserUseCase.signUpWithEmail(FirestoreTestUtil.testName, FirestoreTestUtil.testEmail, FirestoreTestUtil.testPassword)
            if (regResult !is Result.Success)  {
                throw IllegalStateException("Couldn't sign up a user")
            }
            user = regResult.data
        }
    }

    @After
    fun tearDown() = runBlocking {
        FirestoreTestUtil.deleteAuthData()
        FirestoreTestUtil.deleteFirestoreData()
    }

    @Test
    fun saveUser_addsNewsUserIntoFirestore() = runBlocking {
        val saveResult = manageFirestoreUserUseCase.saveUser(user, AuthProvider.EMAIL)
        assertThat(saveResult).isInstanceOf(Result.Success::class.java)

        val getResult = firestoreRepository.getUser(user.uid)
        assertThat(getResult).isInstanceOf(Result.Success::class.java)
        val getUser = (getResult as Result.Success).data
        assertThat(getUser).isNotNull()
        assertThat(getUser.email).isEqualTo(user.email)
        assertThat(getUser.name).isEqualTo(user.displayName)
    }
}