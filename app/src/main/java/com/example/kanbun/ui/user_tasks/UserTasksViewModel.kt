package com.example.kanbun.ui.user_tasks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.TAG
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.TaskRepository
import com.example.kanbun.domain.repository.UserRepository
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.main_activity.MainActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserTasksViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _message = MutableStateFlow<String?>(null)

    val userBoardsState = combine(
        _user,
        _tasks,
        _isLoading,
        _message
    ) { user, tasks, isLoading, message ->
        ViewState.UserTasksViewState(
            user = user,
            tasks = tasks,
            isLoading = isLoading,
            message = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ViewState.UserTasksViewState())

    init {
        viewModelScope.launch {
            getUser()
            Log.d(this@UserTasksViewModel.TAG, "user: ${_user.value}")
            getUserTasks(this@launch)
            Log.d(this@UserTasksViewModel.TAG, "tasks: ${_tasks.value}")
        }
    }

    private suspend fun getUser() {
        userRepository.getUser(MainActivity.firebaseUser!!.uid)
            .onSuccess { user ->
                _user.value = user
            }
            .onError { message, _ ->
                _message.value = message
                _isLoading.value = false
            }
    }

    private suspend fun getUserTasks(scope: CoroutineScope) {
        val tasks = mutableListOf<Task>()
        _user.value?.tasks?.map { entry ->
            val taskListPath = entry.value.substringBeforeLast("/")
            val taskListId = entry.value.substringAfterLast("/")
            scope.async {
                taskRepository.getTask(entry.key, taskListId, taskListPath)
                    .onSuccess { task ->
                        Log.d(this@UserTasksViewModel.TAG, "getUserTasks: task: $task")
                        tasks.add(task)
                    }
                    .onError { message, _ ->
                        _message.value = message
                        _isLoading.value = false
                    }
            }
        }?.awaitAll()
        _isLoading.value = false
        _tasks.value = tasks.sortedBy { it.name }
    }

    fun messageShown() {
        _message.value = null
    }
}