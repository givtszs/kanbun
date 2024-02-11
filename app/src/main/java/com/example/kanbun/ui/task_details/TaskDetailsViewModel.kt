package com.example.kanbun.ui.task_details


import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.ui.BaseViewModel
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.model.TagUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: Add network connectivity checks to this and others view models.

@HiltViewModel
class TaskDetailsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) : BaseViewModel() {
    private var _author = MutableStateFlow(ViewState.TaskDetailsViewState.UserInfo())
    private var _tags = MutableStateFlow<List<TagUi>>(emptyList())
    private var _members = MutableStateFlow<List<Nothing>>(emptyList())
    private var _message = MutableStateFlow<String?>(null)
    private var _loadingManager = MutableStateFlow(ViewState.TaskDetailsViewState.LoadingManager())

    val taskDetailsState: StateFlow<ViewState.TaskDetailsViewState> = combine(
        _author, _tags, _members, _message
    ) { author, tags, members, message ->
        ViewState.TaskDetailsViewState(
            author = author,
            tags = tags,
            members = members,
            message = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ViewState.TaskDetailsViewState())

    private fun messageShown() {
        _message.value = null
    }

    fun getAuthor() {
        _loadingManager.update {
            it.copy(isAuthorLoading = true)
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (firebaseUser == null) {
                _message.value = "The user is null"
                return@launch
            }

            when (val result = firestoreRepository.getUser(firebaseUser.uid)) {
                is Result.Success -> {
                    _author.value = ViewState.TaskDetailsViewState.UserInfo(
                        name = result.data.name ?: "Unknown",
                        profilePicture = result.data.profilePicture
                    )
                    _loadingManager.update { it.copy(isAuthorLoading = false) }
                }

                is Result.Error -> {
                    _message.value = result.message
                    _loadingManager.update { it.copy(isAuthorLoading = false) }
                }
                Result.Loading -> {}
            }
        }
    }

    fun getTags(taskTags: List<String>, boardListPath: String) {
        _loadingManager.update { it.copy(isLoadingTags = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val boardId = boardListPath.substringAfter("boards/").substringBefore("/lists")
            val workspaceId = boardListPath.substringAfter("workspaces/").substringBefore("/boards")
            when (val result = firestoreRepository.getTaskTags(boardId, workspaceId, taskTags)) {
                is Result.Success -> {
                    _tags.value = result.data.map { tag -> TagUi(tag, false) }
                    _loadingManager.update { it.copy(isLoadingTags = false) }
                }
                is Result.Error -> {
                    _message.value = result.message
                    _loadingManager.update { it.copy(isLoadingTags = false) }
                }
                Result.Loading -> {}
            }
        }
    }

}