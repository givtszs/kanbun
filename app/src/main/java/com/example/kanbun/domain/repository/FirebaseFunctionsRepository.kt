package com.example.kanbun.domain.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.functions.HttpsCallableResult

/**
 * Interface defining methods for Firestore interactions related to user data.
 */
interface FirebaseFunctionsRepository {

    suspend fun recursiveDelete(path: String)
}