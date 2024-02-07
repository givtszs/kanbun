package com.example.kanbun.ui.create_task

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
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
    private var _isLoading = MutableStateFlow(false)
    private var _isUpsertingTask = MutableStateFlow(false)
    private var _message = MutableStateFlow<String?>(null)
    val createTaskState: StateFlow<ViewState.CreateTaskViewState> = combine(
        _task, _isLoading, _isUpsertingTask, _message) { task, isLoading, isUpsertingTask, message ->
        ViewState.CreateTaskViewState(
            task = task,
            message = message,
            isLoading = isLoading,
            isUpsertingTask = isUpsertingTask
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ViewState.CreateTaskViewState())

    fun init(task: Task?) {
        _task.value = task

        if (task == null) {
            _message.value = "Task is null"
        }
    }

    fun messageShown() {
        _message.value = null
    }

    fun createTask(task: Task, onSuccessCallback: () -> Unit) = viewModelScope.launch {
        _isUpsertingTask.value = true
        when (val result = firestoreRepository.createTask(
            task = task,
            listId = task.boardListInfo.id,
            listPath = task.boardListInfo.path
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

    fun editTask(updatedTask: Task?, onSuccessCallback: () -> Unit) = viewModelScope.launch {
        Log.d(TAG, "editTask: areTheSame: ${updatedTask == _task.value}, oldTask: ${_task.value}\nupdatedTask: $updatedTask")
        if (updatedTask == _task.value || updatedTask == null) {
            _message.value = "No changes spotted"
            return@launch
        } else {
            _isUpsertingTask.value = true
            when (val result = firestoreRepository.updateTask(updatedTask)) {
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
}
