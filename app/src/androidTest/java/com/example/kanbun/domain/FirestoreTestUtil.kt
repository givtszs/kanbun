package com.example.kanbun.domain

import com.example.kanbun.common.AuthProvider
import com.example.kanbun.common.Role
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.domain.model.WorkspaceInfo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

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

        const val testName = "Test"
        const val testEmail = "qatesteverything@gmail.com"
        const val testPassword = "Qwerty123_"

        /* Test data */
        fun createUser(id: String, tag: String = ""): User {
            return User(
                id = id,
                tag = tag.ifEmpty {
                    "user_" + UUID.randomUUID().toString().substringBefore('-')
                },
                email = "$id@mail.co",
                name = "Test",
                profilePicture = null,
                authProvider = AuthProvider.GOOGLE,
                workspaces = emptyList(),
                sharedWorkspaces = emptyList(),
                sharedBoards = emptyMap(),
                cards = emptyList()
            )
        }

        fun createWorkspace(userId: String, name: String): Workspace =
            Workspace(
                name = name,
                owner = userId,
                members = mapOf(userId to Role.Workspace.Admin),
                boards = emptyList()
            )

        fun createBoard(userId: String, workspace: WorkspaceInfo, name: String): Board =
            Board(
                name = name,
                description = "Simple description",
                owner = userId,
                workspace = workspace,
                members = listOf(Board.BoardMember(userId, Role.Board.Admin))
            )

        fun createBoardList(name: String, position: Int): BoardList =
            BoardList(
                name = name,
                position = position.toLong(),
                tasks = emptyList()
            )

        fun createTask(name: String, position: Long): Task =
            Task(
                position = position,
                name = name,
                description = "Awesome $name task description"
            )

        fun createTag(name: String, color: String): Tag =
            Tag(
                name = name,
                color = color,
            )

        const val black = "#000000"
        const val white = "#FFFFFF"
    }
}