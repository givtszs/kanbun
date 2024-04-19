package com.example.kanbun.data.repository

import android.util.Log
import com.example.kanbun.common.TAG
import com.example.kanbun.di.IoDispatcher
import com.example.kanbun.domain.repository.FirebaseFunctionsRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirebaseFunctionsRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : FirebaseFunctionsRepository {

    /**
     * Calls the Cloud Function to recursively delete a document or collection specified by the [path]
     *
     * @param path the path to the document/collection
     * @throws Exception on failure
     */
    override suspend fun recursiveDelete(path: String) {
        withContext(dispatcher) {
            functions.getHttpsCallable("recursiveDelete")
                .call(hashMapOf("path" to path))
                .addOnSuccessListener {
                    Log.d(TAG, "Deletion succeeded")
                }
                .addOnFailureListener {
                    Log.d(TAG, "Deletion failed")
                    throw it
                }
        }
    }

}