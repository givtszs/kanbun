package com.example.kanbun.data.repository

import android.net.Uri
import android.util.Log
import com.example.kanbun.common.FirebaseStorageFolders
import com.example.kanbun.common.Result
import com.example.kanbun.common.runCatching
import com.example.kanbun.domain.repository.StorageRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,
    private val ioDispatcher: CoroutineDispatcher
) : StorageRepository {

    companion object {
        private const val TAG = "StorageRepository"
    }

    override suspend fun uploadImage(uri: Uri): Result<Unit> = runCatching {
        val file = Uri.fromFile(File(uri.toString()))
        val imageRef = storage.reference.child(
            "${FirebaseStorageFolders.USER_PROFILE_PICTURES}/${uri.lastPathSegment}"
        )
        withContext(ioDispatcher) {
            val uploadTask = imageRef.putFile(file)
            uploadTask.await()
            val fileUrl = imageRef.downloadUrl
            Log.d(TAG, "Uploaded file url: $fileUrl")
        }
    }
}