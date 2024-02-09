package com.example.kanbun.ui.create_task

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.ui.BaseViewModel
import com.example.kanbun.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CreateTaskViewModel"

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) : BaseViewModel() {
    private var _task = MutableStateFlow<Task?>(null)
    private var _tags =
        MutableStateFlow<List<ViewState.CreateTaskViewState.TagUi>>(emptyList())
    private var _isLoading = MutableStateFlow(false)
    private var _isUpsertingTask = MutableStateFlow(false)
    private var _isLoadingTags = MutableStateFlow(false)
    private var _message = MutableStateFlow<String?>(null)

    val createTaskState: StateFlow<ViewState.CreateTaskViewState> = combine(
        _task, _isLoading, _isUpsertingTask, _isLoadingTags, _message
    ) { task, isLoading, isUpsertingTask, isLoadingTags, message ->
        ViewState.CreateTaskViewState(
            task = task,
            message = message,
            isLoading = isLoading,
            isUpsertingTask = isUpsertingTask,
            isLoadingTags = isLoadingTags
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ViewState.CreateTaskViewState())

    fun init(task: Task?, boardListInfo: BoardListInfo) {
        if (task == null) {
            _message.value = "Task is null"
        }

        _task.value = task

        // init tag list
        getTags(boardListInfo)

        // init members list
    }

    fun messageShown() {
        _message.value = null
    }

    fun createTask(task: Task, boardListInfo: BoardListInfo, onSuccessCallback: () -> Unit) =
        viewModelScope.launch {
            _isUpsertingTask.value = true
            when (val result = firestoreRepository.createTask(
                task = task,
                listId = boardListInfo.id,
                listPath = boardListInfo.path
            )) {
                is Result.Success -> {
                    _isUpsertingTask.value = false
                    onSuccessCallback()

                }

                is Result.Error -> {
                    _isUpsertingTask.value = false
                    _message.value = result.message
                }

                Result.Loading -> {}
            }
        }

    fun editTask(updatedTask: Task?, boardListInfo: BoardListInfo, onSuccessCallback: () -> Unit) =
        viewModelScope.launch {
            Log.d(
                TAG,
                "editTask: areTheSame: ${updatedTask == _task.value}, oldTask: ${_task.value}\nupdatedTask: $updatedTask"
            )
            if (updatedTask == _task.value || updatedTask == null) {
                _message.value = "No changes spotted"
                return@launch
            } else {
                _isUpsertingTask.value = true
                when (val result = firestoreRepository.updateTask(
                    updatedTask,
                    boardListInfo.path,
                    boardListInfo.id
                )) {
                    is Result.Success -> {
                        _isUpsertingTask.value = false
                        onSuccessCallback()
                    }

                    is Result.Error -> {
                        _isUpsertingTask.value = false
                        _message.value = result.message
                    }

                    Result.Loading -> {}
                }
            }
        }

    @OptIn(ExperimentalStdlibApi::class)
    fun createTag(name: String, color: Int, boardListInfo: BoardListInfo) = viewModelScope.launch {
        if (_task.value == null) {
            _message.value = "Task is null"
            return@launch
        } else {
            val boardRef =
                boardListInfo.path.substringBefore("/${FirestoreCollection.BOARD_LIST.collectionName}")
            val hexColorCode = color.toHexString()

            when (
                val result = firestoreRepository.createTag(
                    boardPath = boardRef.substringBeforeLast("/"),
                    boardId = boardRef.substringAfterLast("/"),
                    tag = Tag(
                        name = name,
                        textColor = "#$hexColorCode",
                        backgroundColor = "#33$hexColorCode" // 33 - 20% alpha value
                    )
                )
            ) {
                is Result.Success -> getTags(boardListInfo)
                is Result.Error -> _message.value = result.message
                Result.Loading -> {
                    _isLoadingTags.value = true
                }
            }
        }
    }

    fun getTags(boardListInfo: BoardListInfo) = viewModelScope.launch {
        _isLoadingTags.value = true
        val boardRef =
            boardListInfo.path.substringBefore("/${FirestoreCollection.BOARD_LIST.collectionName}")
        when (
            val result = firestoreRepository.getTags(
                boardId = boardRef.substringAfterLast("/"),
                workspaceId = boardRef.substringAfter("${FirestoreCollection.WORKSPACES.collectionName}/")
                    .substringBefore("/${FirestoreCollection.BOARDS.collectionName}")
            )
        ) {
            is Result.Success -> {
                _tags.value =
                    result.data.map { tag -> ViewState.CreateTaskViewState.TagUi(tag, false) }
                _isLoadingTags.value = false
            }

            is Result.Error -> {
                _message.value = result.message
                _isLoadingTags.value = false
            }

            Result.Loading -> _isLoadingTags.value = true
        }
    }
}
