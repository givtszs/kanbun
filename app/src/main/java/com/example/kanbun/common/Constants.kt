package com.example.kanbun.common

import com.example.kanbun.R

enum class FirestoreCollection(val collectionName: String) {
    USERS("users"),
    WORKSPACES("workspaces"),
    BOARDS("boards"),
    BOARD_LIST("lists");

    companion object {
        fun getReference(collection: FirestoreCollection, id: String?) =
            "${collection.collectionName}/$id"
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

object BoardListsAdapterViewType {
    const val VIEW_TYPE_LIST = 0
    const val VIEW_TYPE_CREATE_LIST = 1
}

enum class TaskAction {
    ACTION_CREATE,
    ACTION_EDIT
}

const val EMAIL_RESEND_TIME_LIMIT = 60
const val RECYCLERVIEW_BOARDS_COLUMNS = 2
const val HORIZONTAL_SCROLL_DISTANCE = 5
const val VERTICAL_SCROLL_DISTANCE = 5

val defaultTagColors = listOf(
    "#FF0000", // red
    "#FFA500", // orange
    "#FFFF00", // yellow
    "#EE82EE", // violet
    "#008000", // green
    "#0000FF", // blue
)