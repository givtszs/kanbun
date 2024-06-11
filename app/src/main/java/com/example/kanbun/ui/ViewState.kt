package com.example.kanbun.ui

import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.TaskList
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.ui.model.Member
import com.example.kanbun.ui.model.TagUi
import com.example.kanbun.ui.model.UserSearchResult
import com.google.android.material.textfield.TextInputLayout

sealed class ViewState {

    data class AuthState(
        val nameError: String? = null,
        val emailError: String = "",
        val passwordError: String = "",
        val confirmationPasswordError: String? = null,
        val message: String? = null
    ) : ViewState() {
        /**
         * Handles the text field error state
         */
        fun processError(error: String?, textField: TextInputLayout) {
            if (!error.isNullOrEmpty()) {
                textField.apply {
                    setError(error)
                    isErrorEnabled = true
                }
            }
        }
    }

    data class EmailVerificationState(
        val isResendAvailable: Boolean = true,
        val countdownMillis: Int = 0,
        val message: String? = null
    ) : ViewState()

    data class UserBoardsState(
        val message: String? = null,
        val isLoading: Boolean = true,
        val workspace: WorkspaceState = WorkspaceState.NullWorkspace
    ) : ViewState()

    data class DrawerState(
        val user: User? = null,
        val selectedWorkspace: String? = null
    ) : ViewState()


    sealed interface WorkspaceState {
        data object NullWorkspace : WorkspaceState
        data class WorkspaceReady(val workspace: Workspace) : WorkspaceState
    }

    data class UserTasksViewState(
        val user: User? = null,
        val tasks: List<Task> = emptyList(),
        val isLoading: Boolean = true,
        val message: String? = null
    ) : ViewState()

    data class WorkspaceSettingsViewState(
        val members: List<Member> = emptyList(),
        val foundUsers: List<UserSearchResult>? = null,
        val isLoading: Boolean = false,
        val message: String? = null
    ) : ViewState()

    data class BoardViewState(
        val board: Board = Board(),
        val lists: List<TaskList> = emptyList(),
        val members: List<User> = emptyList(),
        val isLoading: Boolean = false,
        val message: String? = null
    ) : ViewState()

    data class TaskEditorViewState(
        val tags: List<TagUi> = mutableListOf(),
        val message: String? = null,
        val isSavingTask: Boolean = false,
        val taskMembers: List<User> = emptyList(),
        val foundUsers: List<UserSearchResult>? = null
    ) : ViewState()

    data class TaskDetailsViewState(
        val author: User? = null,
        val message: String? = null
    ) : ViewState()

    data class BoardSettingsViewState(
        val isLoading: Boolean = false,
        val message: String? = null,
        val tags: List<TagUi> = emptyList(),
        val boardMembers: List<Member> = emptyList(),
        val foundUsers: List<UserSearchResult>? = null
    ) : ViewState()

    data class EditTagsViewState(
        val tags: List<Tag> = emptyList(),
        val message: String? = null
    ) : ViewState()

    data class EditProfileViewState(
        val user: User? = null,
        val message: String? = null,
        val isLoading: Boolean = true,
        val isUploadingImage: Boolean = false,
        val nameError: String? = null,
        val tagError: String? = null
    ) : ViewState()
}