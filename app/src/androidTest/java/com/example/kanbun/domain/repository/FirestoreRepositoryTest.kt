package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.data.repository.FirestoreRepositoryImpl
import com.example.kanbun.domain.FirestoreTestUtil
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class FirestoreRepositoryTest {
    private lateinit var repository: FirestoreRepository

    @Before
    fun setUp() {
        val firestore = FirestoreTestUtil.db
        repository = FirestoreRepositoryImpl(firestore)
    }

    @After
    fun tearDown() = runBlocking {
        FirestoreTestUtil.deleteData()
    }

    @Test
    fun addUser_addsSignedInUserData() = runBlocking {
        val user = FirestoreTestUtil.generateUser()
        val result = repository.addUser(user)

        assertThat(result).isInstanceOf(Result.Success::class.java)

        // suppose the result is always successful
        val savedUser = (repository.getUser(user.uid) as Result.Success).data

        // test the user is saved correctly
        assertThat(savedUser).isEqualTo(user)
    }

    @Test
    fun getUser_getsSignedInUserData() = runBlocking {
        val user = FirestoreTestUtil.generateUser()
        repository.addUser(user)

        val result = repository.getUser(user.uid)
        assertThat(result).isInstanceOf(Result.Success::class.java)

        val resultUser = (result as Result.Success).data
        assertThat(resultUser).isEqualTo(user)
    }

    @Test
    fun getUser_toGetNonexistentUser_returnsResultError() = runBlocking {
        val userId = "nonexistent_id"

        val result = repository.getUser(userId)
        assertThat(result).isInstanceOf(Result.Error::class.java)

        val resultMessage = (result as Result.Error).message
        assertThat(resultMessage).isNotNull()
    }
}