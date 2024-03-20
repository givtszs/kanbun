package com.example.kanbun.ui.create_task

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.common.TaskAction
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.domain.usecase.UpsertTagUseCase
import com.example.kanbun.ui.BaseViewModel
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.model.TagUi
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
    private val firestoreRepository: FirestoreRepository,
    private val upsertTagUseCase: UpsertTagUseCase
) : BaseViewModel() {
    private var _task = MutableStateFlow<Task?>(null)
    private var _tags =
        MutableStateFlow<List<TagUi>>(emptyList())
    private var _message = MutableStateFlow<String?>(null)

    private var _isScreenLoading = MutableStateFlow(false)
    private var _isUpsertingTask = MutableStateFlow(false)
    private var _isLoadingTags = MutableStateFlow(false)

    private var _loadingManager = combine(
        _isScreenLoading, _isUpsertingTask, _isLoadingTags
    ) { isLoading, isUpsertingTask, isLoadingTags ->
        ViewState.CreateTaskViewState.LoadingManager(
            isScreenLoading = isLoading,
            isUpsertingTask = isUpsertingTask,
            isLoadingTags = isLoadingTags
        )
    }

    val createTaskState: StateFlow<ViewState.CreateTaskViewState> = combine(
        _task, _tags, _message, _loadingManager
    ) { task, tags, message, loadingManager ->
        ViewState.CreateTaskViewState(
            task = task,
            tags = tags,
            message = message,
            loadingManager = loadingManager
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ViewState.CreateTaskViewState())

    fun init(task: Task?, boardListInfo: BoardListInfo, taskAction: TaskAction) {
        if (task == null) {
            _message.value = "Task is null"
        }

        _task.value = task

        viewModelScope.launch {
            // init tag list
            getTags(boardListInfo)

            if (taskAction == TaskAction.ACTION_EDIT) {
                _tags.value.filter { it.tag.id in task!!.tags }.forEach { it.isSelected = true }
            }
        }


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

    fun createTag(tag: Tag, boardListInfo: BoardListInfo) {
        viewModelScope.launch {
            val boardRef =
                boardListInfo.path.substringBefore("/${FirestoreCollection.BOARD_LIST.collectionName}")

            when (
                val result = upsertTagUseCase(
                    tag = tag,
                    tags = _tags.value.map { it.tag },
                    boardPath = boardRef.substringBeforeLast("/"),
                    boardId = boardRef.substringAfterLast("/"),
                )
            ) {
                is Result.Success -> _tags.value = _tags.value + TagUi(result.data, false)
                is Result.Error -> _message.value = result.message
                Result.Loading -> _isLoadingTags.value = true
            }
        }
    }


    private suspend fun getTags(boardListInfo: BoardListInfo) {
        _isLoadingTags.value = true
        val boardRef =
            boardListInfo.path.substringBefore("/${FirestoreCollection.BOARD_LIST.collectionName}")
        val workspaceId =
            boardRef.substringAfter("${FirestoreCollection.WORKSPACES.collectionName}/")
                .substringBefore("/${FirestoreCollection.BOARDS.collectionName}")

        when (
            val result = firestoreRepository.getAllTags(
                boardId = boardRef.substringAfterLast("/"),
                workspaceId = workspaceId
            )
        ) {
            is Result.Success -> {
                _tags.value = result.data.map { tag -> TagUi(tag, false) }
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
