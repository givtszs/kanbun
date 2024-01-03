package com.example.kanbun.domain.repository

import com.example.kanbun.common.FirestoreEnvironment
import com.example.kanbun.data.repository.FirestoreRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class FirestoreRepositoryTest {
    private lateinit var repository: FirestoreRepository

    @Before
    fun setUp() {
        repository = FirestoreRepositoryImpl(FirestoreEnvironment.TEST)
    }
}