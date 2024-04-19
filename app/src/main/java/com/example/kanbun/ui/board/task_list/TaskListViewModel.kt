package com.example.kanbun.ui.board.task_list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.common.TAG
import com.example.kanbun.domain.model.TaskList
import com.example.kanbun.domain.repository.TaskListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskListRepository: TaskListRepository
) : ViewModel() {

    fun editTaskListName(
        newName: String,
        taskListPath: String,
        taskListId: String,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        when (val result = taskListRepository.updateTaskListName(newName, taskListPath, taskListId)) {
            is Result.Success -> onSuccess()
            is Result.Error -> Log.d(TAG, "${result.message}")
        }

    }

    fun deleteTaskList(
        taskList: TaskList,
        taskLists: List<TaskList>,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        when (taskListRepository.deleteTaskListAndRearrange(
            id = taskList.id,
            path = taskList.path,
            taskLists = taskLists,
            deleteAt = taskList.position.toInt()
        )) {
            is Result.Success -> onSuccess()
            is Result.Error -> {}
        }

    }
}