package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.FirestoreRepository
import javax.inject.Inject

class SearchUserUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) {
    suspend operator fun invoke(tag: String): Result<List<User>> {
       return firestoreRepository.findUsersByTag(tag)
    }
}