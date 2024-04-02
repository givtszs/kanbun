package com.example.kanbun.common

import com.example.kanbun.ui.model.ItemRole
import com.squareup.moshi.Moshi
import kotlinx.parcelize.RawValue

object FirestoreCollection {
    const val USERS = "users"
    const val WORKSPACES = "workspaces"
    const val BOARDS = "boards"
    const val TASK_LISTS = "lists"
    const val TASKS = "tasks"
}

enum class AuthProvider(val providerId: String) {
    EMAIL("password"),
    GOOGLE("google.com"),
    GITHUB("github.com")
}

sealed class Role(val name: String, val description: String) {

     sealed class Workspace(roleName: String, roleDescription: String) : Role(roleName, roleDescription) {
         data object Admin : Workspace("Admin", "Description of the Workspace Admin role")
         data object Member : Workspace("Member", "Description of the Workspace Member role")
     }

    sealed class Board(roleName: String, roleDescription: String) : Role(roleName, roleDescription) {
        data object Admin : Board("Admin", "Description of the Board Admin role")
        data object Member : Board("Member", "Description of the Workspace Member role")
    }
}

//val boardRoles = listOf(
//    ItemRole(BoardRole.ADMIN.roleName, BoardRole.ADMIN.roleDescription),
//    ItemRole(BoardRole.MEMBER.roleName, BoardRole.ADMIN.roleDescription),
//)
//
//enum class BoardRole(val roleName: String, val roleDescription: String) {
//    ADMIN("Admin", "Description of the role"),
//    MEMBER("Member", "Description of the role")
//}

object ToastMessage {
    const val NO_NETWORK_CONNECTION = "No Internet connection"
    const val SIGN_IN_SUCCESS = "Signed in successfully"
    const val SIGN_UP_SUCCESS = "Signed up successfully"
    const val EMAIL_VERIFIED = "Email has been verified"
    const val WORKSPACE_CREATED = "Workspace has been created"
}

object WorkspaceType {
    const val USER_WORKSPACE = "workspaces"
    const val SHARED_WORKSPACE = "sharedWorkspaces"
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

object DrawerItem {
    const val SHARED_BOARDS = "shared_boards"
}

val defaultTagColors = listOf(
    "#FF0000", // red
    "#FFA500", // orange
    "#FFFF00", // yellow
    "#EE82EE", // violet
    "#008000", // green
    "#0000FF", // blue
)

val moshi: Moshi = Moshi.Builder().build()