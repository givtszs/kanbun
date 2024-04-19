package com.example.kanbun.domain.repository

import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.google.firebase.functions.HttpsCallableResult

/**
 * Interface defining methods for Firestore interactions related to user data.
 */
interface FirestoreRepository {

    fun recursiveDelete(path: String): com.google.android.gms.tasks.Task<HttpsCallableResult>

    suspend fun upsertTag(
        tag: Tag,
        boardId: String,
        boardPath: String
    ): Result<Tag>
}