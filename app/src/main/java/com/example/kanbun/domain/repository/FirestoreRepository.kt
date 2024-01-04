package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.User

interface FirestoreRepository {
    suspend fun addUser(user: User): Result<Unit>
    suspend fun getUser(userId: String): Result<User>
}