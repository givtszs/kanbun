package com.example.kanbun.domain.usecase

import android.net.Uri
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.StorageRepository
import javax.inject.Inject

class UpdateProfilePictureUseCase @Inject constructor(
    private val storageRepository: StorageRepository,
    private val updateUserUseCase: UpdateUserUseCase
) {
    suspend operator fun invoke(user: User, uri: Uri): Result<Unit> {
        val uploadImageResult = storageRepository.uploadImage(uri)
        if (uploadImageResult is Result.Error) {
            return Result.Error(uploadImageResult.message, uploadImageResult.e)
        }
        val imagePath = (uploadImageResult as Result.Success).data
        // update user entry in the firestore
        val updatedUser = user.copy(profilePicture = imagePath)
        return updateUserUseCase(user, updatedUser)
    }
}