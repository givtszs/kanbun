package com.example.kanbun.domain.usecase

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.ui.main_activity.MainActivity
import javax.inject.Inject

class GetUserUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) {

    suspend operator fun invoke(): Result<User> {
        return firestoreRepository.getUser(MainActivity.firebaseUser!!.uid)
    }
}