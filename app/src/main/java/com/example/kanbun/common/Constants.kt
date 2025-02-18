package com.example.kanbun.common

import android.os.Parcelable
import androidx.annotation.StringRes
import com.example.kanbun.R
import com.squareup.moshi.Moshi
import kotlinx.parcelize.Parcelize

object FirestoreCollection {
    const val USERS = "users"
    const val WORKSPACES = "workspaces"
    const val BOARDS = "boards"
    const val TASK_LISTS = "lists"
    const val TASKS = "tasks"
}

object FirebaseStorageFolders {
    const val USER_PROFILE_PICTURES = "user_prof_pics"
}

enum class AuthProvider(val providerId: String) {
    EMAIL("password"),
    GOOGLE("google.com"),
    GITHUB("github.com")
}

@Parcelize
sealed class Role(val name: String, @StringRes val description: Int) : Parcelable {

    @Parcelize
    sealed class Workspace(private val roleName: String, private val roleDescription: Int) : Role(roleName, roleDescription) {
        @Parcelize
        data object Admin : Workspace("Admin", R.string.workspace_role_admin_description)
        @Parcelize
        data object Member : Workspace("Member", R.string.workspace_role_member_description)
    }

    @Parcelize
    sealed class Board(private val roleName: String, private val roleDescription: Int) : Role(roleName, roleDescription) {
        @Parcelize
        data object Admin : Board("Admin", R.string.board_role_admin_description)
        @Parcelize
        data object Member : Board("Member", R.string.board_role_member_description)
    }
}

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