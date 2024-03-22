package com.example.kanbun.common

import com.squareup.moshi.Moshi

object FirestoreCollection {
    const val USERS = "users"
    const val WORKSPACES = "workspaces"
    const val BOARDS = "boards"
    const val TASK_LISTS = "lists"
    const val TASKS = "tasks"

    fun getBoardsPath(workspaceId: String): String {
        return "$WORKSPACES/$workspaceId/$BOARDS"
    }

    fun getBoardReference(boardId:String, workspaceId: String): String {
        return "$WORKSPACES/$workspaceId/$BOARDS/$boardId"
    }
}

enum class AuthProvider(val providerId: String) {
    EMAIL("password"),
    GOOGLE("google.com"),
    GITHUB("github.com")
}

enum class WorkspaceRole(val roleName: String) {
    ADMIN("Admin"),
    MEMBER("Member")
}

enum class BoardRole(val roleName: String) {
    ADMIN("Admin"),
    MEMBER("Member")
}

object ToastMessage {
    const val NO_NETWORK_CONNECTION = "No Internet connection"
    const val SIGN_IN_SUCCESS = "Signed in successfully"
    const val SIGN_UP_SUCCESS = "Signed up successfully"
    const val EMAIL_VERIFIED = "Email has been verified"
    const val WORKSPACE_CREATED = "Workspace has been created"
}

enum class TaskAction {
    ACTION_CREATE,
    ACTION_EDIT
}

const val EMAIL_RESEND_TIME_LIMIT = 60
const val RECYCLERVIEW_BOARDS_COLUMNS = 2
const val HORIZONTAL_SCROLL_DISTANCE = 5
const val DATE_FORMAT = "dd MMM yyyy"
const val TIME_FORMAT = "HH:mm"
const val DATE_TIME_FORMAT = "dd MMM yyyy, HH:mm"

val defaultTagColors = listOf(
    "#FF0000", // red
    "#FFA500", // orange
    "#FFFF00", // yellow
    "#EE82EE", // violet
    "#008000", // green
    "#0000FF", // blue
)

val moshi: Moshi = Moshi.Builder().build()