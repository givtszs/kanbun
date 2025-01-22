package com.example.kanbun.ui.edit_task

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.common.TAG
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.TaskListInfo
import com.example.kanbun.domain.repository.TaskRepository
import com.example.kanbun.domain.usecase.UpsertTagUseCase
import com.example.kanbun.ui.task_editor.TaskEditorViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    upsertTagUseCase: UpsertTagUseCase,
) : TaskEditorViewModel(upsertTagUseCase) {

    fun editTask(oldTask: Task, updatedTask: Task, taskListInfo: TaskListInfo, onSuccessCallback: () -> Unit) =
        viewModelScope.launch {
            Log.d(TAG, "editTask: areTheSame: ${updatedTask == oldTask}\noldTask: $oldTask\nupdatedTask: $updatedTask")
            if (updatedTask == oldTask) {
                _message.value = "No changes spotted"
                onSuccessCallback()
                return@launch
            } else {
                _isSavingTask.value = true
                when (
                    val result = taskRepository.updateTask(
                        oldTask = oldTask,
                        newTask = updatedTask,
                        taskListId = taskListInfo.id,
                        taskListPath = taskListInfo.path
                    )
                ) {
                    is Result.Success -> {
                        _isSavingTask.value = false
                        onSuccessCallback()
                    }

                    is Result.Error -> {
                        _isSavingTask.value = false
                        _message.value = result.message
                    }
                }
            }
        }

}