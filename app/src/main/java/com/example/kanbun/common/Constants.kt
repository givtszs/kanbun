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
const val COLOR_GRID_COLUMNS = 5

object DrawerItem {
    const val SHARED_BOARDS = "shared_boards"
}
//
//val defaultTagColors = listOf(
//    "#FF0000", // red
//    "#FFA500", // orange
//    "#FFFF00", // yellow
//    "#EE82EE", // violet
//    "#008000", // green
//    "#0000FF", // blue
//)

val tagColors = mapOf(
    0 to R.color.tag_red,
    1 to R.color.tag_rose,
    2 to R.color.tag_pink,
    3 to R.color.tag_fuscia,
    4 to R.color.tag_purple,
    5 to R.color.tag_indigo,
    6 to R.color.tag_blue,
    7 to R.color.tag_sky,
    8 to R.color.tag_cyan,
    9 to R.color.tag_teal,
    10 to R.color.tag_emerald,
    11 to R.color.tag_green,
    12 to R.color.tag_yellow,
    13 to R.color.tag_amber,
    14 to R.color.tag_hues,
)

val moshi: Moshi = Moshi.Builder().build()