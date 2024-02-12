package com.example.kanbun.ui.task_details


import android.util.Log
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

private const val TAG = "TaskDetailsViewModel"

@HiltViewModel
class TaskDetailsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) : BaseViewModel() {
    private var _author = MutableStateFlow(ViewState.TaskDetailsViewState.UserInfo())
    private var _tags = MutableStateFlow<List<TagUi>>(emptyList())
    private var _members = MutableStateFlow<List<Nothing>>(emptyList())
    private var _message = MutableStateFlow<String?>(null)

    private var _isLoadingAuthor = MutableStateFlow(true)
    private var _isLoadingTags = MutableStateFlow(true)
    private var _isLoadingMembers = MutableStateFlow(true)

    private var _isLoading = combine(
        _isLoadingAuthor, _isLoadingTags, _isLoadingMembers
    ) { isLoadingAuthor, isLoadingTags, isLoadingMembers ->
        Log.d(TAG, "isLoadingAuthor: $isLoadingAuthor, isLoadingTags: $isLoadingTags, isLoadingMembers: $isLoadingMembers")
        !(!isLoadingAuthor && !isLoadingTags && !isLoadingMembers)
    }

    val taskDetailsState: StateFlow<ViewState.TaskDetailsViewState> = combine(
        _author, _tags, _members, _message, _isLoading
    ) { author, tags, members, message, isLoading ->
        ViewState.TaskDetailsViewState(
            author = author,
            tags = tags,
            members = members,
            message = message,
            isLoading = isLoading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ViewState.TaskDetailsViewState())

    private fun messageShown() {
        _message.value = null
    }

    fun getAuthor() {
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
                    _isLoadingAuthor.value = false
                }

                is Result.Error -> {
                    _message.value = result.message
                    _isLoadingAuthor.value = false
                }
                Result.Loading -> {}
            }
        }
    }

    fun getTags(taskTags: List<String>, boardListPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val boardId = boardListPath.substringAfter("boards/").substringBefore("/lists")
            val workspaceId = boardListPath.substringAfter("workspaces/").substringBefore("/boards")
            when (val result = firestoreRepository.getTaskTags(boardId, workspaceId, taskTags)) {
                is Result.Success -> {
                    _tags.value = result.data.map { tag -> TagUi(tag, false) }
                    _isLoadingTags.value = false
                }
                is Result.Error -> {
                    _message.value = result.message
                    _isLoadingTags.value = false
                }
                Result.Loading -> {}
            }
        }
    }

    fun getMembers() {
        _isLoadingMembers.value = false
    }
}