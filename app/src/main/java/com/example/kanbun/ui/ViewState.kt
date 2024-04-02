package com.example.kanbun.ui

import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
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

    data class UserBoardsViewState(
        val user: User? = null,
        val currentWorkspace: Workspace? = null,
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
        val lists: List<BoardList> = emptyList(),
        val isLoading: Boolean = true,
        val message: String? = null
    ) : ViewState()

    data class CreateTaskViewState(
        val task: Task? = null,
        val tags: List<TagUi> = mutableListOf(),
        val loadingManager: LoadingManager = LoadingManager(), // TODO: Change to the single isLoading property
        val message: String? = null,
        val taskMembers: List<User> = emptyList(),
        val foundUsers: List<User>? = null
    ) : ViewState() {
        data class LoadingManager(
            val isScreenLoading: Boolean = false,
            val isUpsertingTask: Boolean = false,
            val isLoadingTags: Boolean = false
        )
    }

    data class TaskDetailsViewState(
        val author: UserInfo = UserInfo(),
        val tags: List<TagUi> = emptyList(),
        val members: List<Nothing> = emptyList(), // TODO: update generic type with the member model
        val message: String? = null,
        val isLoading: Boolean = false
    ) : ViewState() {

        data class UserInfo(
            val name: String = "",
            val profilePicture: String? = null
        )
    }

    data class BoardSettingsViewState(
        val isLoading: Boolean = false,
        val message: String? = null,
        val tags: List<TagUi> = emptyList(),
        val boardMembers: List<Member> = emptyList(),
        val foundUsers: List<User>? = null
    ) : ViewState()

    data class EditTagsViewState(
        val tags: List<Tag> = emptyList(),
        val message: String? = null
    ) : ViewState()
}
