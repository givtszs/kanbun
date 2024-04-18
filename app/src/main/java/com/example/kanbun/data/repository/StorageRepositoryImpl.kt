package com.example.kanbun.data.repository

import android.net.Uri
import android.util.Log
import com.example.kanbun.common.FirebaseStorageFolders
import com.example.kanbun.common.Result
import com.example.kanbun.common.runCatching
import com.example.kanbun.di.IoDispatcher
import com.example.kanbun.domain.repository.StorageRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : StorageRepository {

    companion object {
        private const val TAG = "StorageRepository"
    }

    override suspend fun uploadImage(uri: Uri): Result<String> = runCatching {
        val imageRef = storage.reference
            .child("${FirebaseStorageFolders.USER_PROFILE_PICTURES}/${uri.lastPathSegment}")
        Log.d(TAG, "Image path: $imageRef")
        withContext(ioDispatcher) {
            val uploadTask = imageRef.putFile(uri)
            uploadTask.await()
        }
        imageRef.toString()
    }
}