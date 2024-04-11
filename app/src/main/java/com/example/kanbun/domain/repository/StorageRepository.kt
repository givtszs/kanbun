package com.example.kanbun.domain.repository

import android.net.Uri
import com.example.kanbun.common.Result

interface StorageRepository {

    suspend fun uploadImage(uri: Uri): Result<Unit>
}