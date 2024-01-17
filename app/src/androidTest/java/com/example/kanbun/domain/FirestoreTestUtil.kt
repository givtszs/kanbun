package com.example.kanbun.domain

import com.example.kanbun.common.AuthProvider
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.WorkspaceRole
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.UserWorkspace
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.model.WorkspaceMember
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class FirestoreTestUtil {
    companion object {
        private const val host = "10.0.2.2"
        private const val firestorePort = 8080
        private const val authPort = 9099
        private const val projectName = "kanbun-aa2d6"

        val firestore = Firebase.firestore.apply {
            useEmulator(host, firestorePort)
            firestoreSettings = firestoreSettings {
                isPersistenceEnabled = false
            }
        }

        val auth = Firebase.auth.apply {
            useEmulator(host, authPort)
        }

        suspend fun deleteFirestoreData() {
            val url =
                URL("http://$host:$firestorePort/emulator/v1/projects/${projectName}/databases/(default)/documents")
            delete(url)
        }

        suspend fun deleteAuthData() {
            val url =
                URL("http://$host:$authPort/emulator/v1/projects/${projectName}/accounts")
            delete(url)
        }

        private suspend fun delete(url: URL) = withContext(Dispatchers.IO) {
            val con = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                instanceFollowRedirects = false
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            con.responseCode
            con.disconnect()
        }

        /* Test data */

        val user = User(
            id = "test1",
            email = "test@gmail.com",
            name = "Test",
            profilePicture = null,
            authProvider = AuthProvider.GOOGLE,
            workspaces = listOf(UserWorkspace("workspaces/work1", "Test Workspace 1")),
            cards = listOf("workspaces/work1/boards/board1/columns/col1/card1")
        )

        val userEmptyWorksAndCards = User(
            id = "test1",
            email = "test@gmail.com",
            name = "Test",
            profilePicture = null,
            authProvider = AuthProvider.GOOGLE,
            workspaces = emptyList(),
            cards = emptyList()
        )

        const val testName = "IAmTest"
        const val testEmail = "qatesteverything@gmail.com"
        const val testPassword = "Qwerty123_"

        val workspace = Workspace(
            name = "Test",
            owner = FirestoreCollection.getReference(
                FirestoreCollection.USERS,
                userEmptyWorksAndCards.id
            ),
            members = listOf(
                WorkspaceMember(
                    FirestoreCollection.getReference(FirestoreCollection.USERS, userEmptyWorksAndCards.id),
                    WorkspaceRole.ADMIN
                )
            ),
            boards = emptyList()
        )
    }
}