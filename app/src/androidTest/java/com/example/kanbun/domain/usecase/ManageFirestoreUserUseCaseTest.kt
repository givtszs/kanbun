package com.example.kanbun.domain.usecase

import com.example.kanbun.common.AuthProvider
import com.example.kanbun.common.Result
import com.example.kanbun.data.repository.FirebaseFunctionsRepositoryImpl
import com.example.kanbun.domain.FirestoreTestUtil
import com.example.kanbun.domain.repository.AuthenticationRepository
import com.example.kanbun.domain.repository.FirebaseFunctionsRepository
import com.example.kanbun.isResultSuccess
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class ManageFirestoreUserUseCaseTest {
    private lateinit var manageFirestoreUserUseCase: ManageFirestoreUserUseCase
    private lateinit var firestoreRepository: FirebaseFunctionsRepository
    private lateinit var authRepository: AuthenticationRepository
    private val auth = FirestoreTestUtil.auth
    private lateinit var user: FirebaseUser

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        val firestore = FirestoreTestUtil.firestore
        val dispatcher = UnconfinedTestDispatcher()
        firestoreRepository = FirebaseFunctionsRepositoryImpl(firestore, dispatcher)
        manageFirestoreUserUseCase = ManageFirestoreUserUseCase(firestoreRepository)
    }

    @After
    fun tearDown() = runBlocking {
//        FirestoreTestUtil.deleteFirestoreData()
    }

    @Test
    fun saveUser_addsNewsUserIntoFirestore() = runBlocking {
        val user = Mockito.mock(FirebaseUser::class.java)
        `when`(user.uid).thenReturn("your-uid")
        `when`(user.email).thenReturn("example@example.com")
        `when`(user.displayName).thenReturn("Example User")
        `when`(user.photoUrl).thenReturn(null)
        val saveResult = manageFirestoreUserUseCase.saveUser(user, AuthProvider.EMAIL)

        assertThat(saveResult).isResultSuccess()

        val getResult = firestoreRepository.getUser(user.uid)

        assertThat(getResult).isResultSuccess()


        val getUser = (getResult as Result.Success).data

        assertThat(getUser).isNotNull()
        assertThat(getUser.email).isEqualTo(user.email)
        assertThat(getUser.name).isEqualTo(user.displayName)
    }
}