package com.example.kanbun.ui.task_editor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.usecase.UpsertTagUseCase
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.model.Member
import com.example.kanbun.ui.model.TagUi
import com.example.kanbun.ui.model.UserSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class TaskEditorViewModel @Inject constructor(
    private val upsertTagUseCase: UpsertTagUseCase,
) : ViewModel() {

    companion object {
        private const val TAG = "TaskEditorViewModel"
    }

    private var boardMembers: List<User> = emptyList()

    private var _tags = MutableStateFlow<List<TagUi>>(emptyList())
    protected var _message = MutableStateFlow<String?>(null)
    protected var _isSavingTask = MutableStateFlow(false)
    private var _taskMembers = MutableStateFlow<List<User>>(emptyList())
    private var _foundUsers = MutableStateFlow<List<UserSearchResult>?>(null)

    val taskEditorState: StateFlow<ViewState.TaskEditorViewState> = combine(
        _tags, _message, _isSavingTask, _taskMembers, _foundUsers
    ) { tags, message, isSavingTask, taskMembers, foundUsers ->
        Log.d(TAG, "createTaskState: tags: $tags")
        ViewState.TaskEditorViewState(
            tags = tags,
            message = message,
            isSavingTask = isSavingTask,
            taskMembers = taskMembers,
            foundUsers = foundUsers
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ViewState.TaskEditorViewState())

    fun init(
        task: Task?,
        boardMembers: List<User>,
        tags: List<Tag>
    ) {
        this.boardMembers = boardMembers
        _tags.value = tags.map { tag ->
            TagUi(
                tag = tag,
                isSelected = task?.tags?.any { tag.id == it } ?: false
            )
        }

        if (task != null) {
            _taskMembers.value = boardMembers.filter { member -> member.id in task.members }
        }

        Log.d(TAG, "task: $task,\nboardMembers: $boardMembers,\ntags: ${_tags.value},\ntaskMembers: ${_taskMembers.value}")
    }

    fun messageShown() {
        _message.value = null
    }

    fun createTag(tag: Tag, boardListInfo: BoardListInfo, onSuccess: (Tag) -> Unit) {
        viewModelScope.launch {
            val boardRef =
                boardListInfo.path.substringBefore("/${FirestoreCollection.TASK_LISTS}")

            when (
                val result = upsertTagUseCase(
                    tag = tag,
                    tags = _tags.value.map { it.tag },
                    boardPath = boardRef.substringBeforeLast("/"),
                    boardId = boardRef.substringAfterLast("/"),
                )
            ) {
                is Result.Success -> {
                    val newTag = result.data
                    _tags.value = _tags.value + TagUi(newTag, false)
                    onSuccess(newTag)
                }
                is Result.Error -> _message.value = result.message
            }
        }
    }

    fun searchUser(tag: String) {
        Log.d(TAG, "searchUser: tag: $tag")
        _foundUsers.value = boardMembers.filter { it.tag.contains(tag) }.map { user ->
            UserSearchResult(
                user,
                isAdded = _taskMembers.value.contains(user)
            )
        }
        Log.d(TAG, "searchUser: foundUsers: ${_foundUsers.value}")
    }

    fun resetFoundUsers(clear: Boolean = false) {
        Log.d(TAG, "resetFoundUsers: clear: $clear")
        _foundUsers.value = if (clear) {
            null
        } else {
            boardMembers.map { user ->
                UserSearchResult(
                    user,
                    _taskMembers.value.any { it.id == user.id }
                )
            }
        }

        Log.d(TAG, "resetFoundUsers: foundUsers: ${_foundUsers.value}")
    }

    fun addMember(user: User) {
        if (!_taskMembers.value.contains(user)) {
            _taskMembers.update { it + user }
        }
    }

    fun removeMember(member: User) {
        _taskMembers.update { _member -> _member.filterNot { it == member } }
    }

    fun setMembers(members: List<Member>) {
        _taskMembers.value = members.map { it.user }
    }
}