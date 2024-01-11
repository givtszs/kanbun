package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.data.repository.FirestoreRepositoryImpl
import com.example.kanbun.domain.FirestoreTestUtil
import com.example.kanbun.domain.repository.FirestoreRepository
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.runBlocking
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

    @Before
    fun setUp() {
        registerUserUseCase = RegisterUserUseCase(auth)

        val firestore = FirestoreTestUtil.firestore
        firestoreRepository = FirestoreRepositoryImpl(firestore)
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
//        FirestoreTestUtil.deleteFirestoreData()
    }

    @Test
    fun saveUser_addsNewsUserIntoFirestore() = runBlocking {
        val saveResult = manageFirestoreUserUseCase.saveUser(user)
        assertThat(saveResult).isInstanceOf(Result.Success::class.java)

        val getResult = firestoreRepository.getUser(user.uid)
        assertThat(getResult).isInstanceOf(Result.Success::class.java)
        val getUser = (getResult as Result.Success).data
        assertThat(getUser).isNotNull()
        assertThat(getUser.email).isEqualTo(user.email)
        assertThat(getUser.name).isEqualTo(user.displayName)
        assertThat(getUser.profilePicture).isNull()
    }
}