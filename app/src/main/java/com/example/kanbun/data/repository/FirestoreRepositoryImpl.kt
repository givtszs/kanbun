package com.example.kanbun.data.repository

import com.example.kanbun.common.FirestoreEnvironment
import com.example.kanbun.domain.repository.FirestoreRepository
import javax.inject.Inject

class FirestoreRepositoryImpl @Inject constructor(
    private val environment: FirestoreEnvironment
) : FirestoreRepository {

}