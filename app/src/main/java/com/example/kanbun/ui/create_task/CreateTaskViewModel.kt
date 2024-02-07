package com.example.kanbun.ui.create_task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.BoardListInfo
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

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) : BaseViewModel() {
    private var _task = MutableStateFlow<Task?>(null)
    private var _isLoading = MutableStateFlow(false)
    private var _isTaskCreating = MutableStateFlow(false)
    private var _message = MutableStateFlow<String?>(null)
    val createTaskState: StateFlow<ViewState.CreateTaskViewState> = combine(
        _task, _isLoading, _isTaskCreating, _message) { task, isLoading, isTaskCreating, message ->
        ViewState.CreateTaskViewState(
            task = task,
            message = message,
            isLoading = isLoading,
            isTaskCreating = isTaskCreating
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
        _isTaskCreating.value = true
        when (val result = firestoreRepository.createTask(
            task = task,
            listId = task.boardListInfo.id,
            listPath = task.boardListInfo.path
        )) {
            is Result.Success -> {
                _isTaskCreating.value = false
                onSuccessCallback()

            }

            is Result.Error -> {
                _isTaskCreating.value = false
                _message.value = result.message
            }

            Result.Loading -> {}
        }
    }

    fun editTask(task: Task, onSuccessCallback: () -> Unit) = viewModelScope.launch {

    }
}
