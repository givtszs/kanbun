package com.example.kanbun.ui.create_task

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.domain.usecase.UpsertTagUseCase
import com.example.kanbun.ui.model.TagUi
import com.example.kanbun.ui.task_editor.TaskEditorViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    upsertTagUseCase: UpsertTagUseCase,
) : TaskEditorViewModel(upsertTagUseCase) {

    companion object {
        private const val TAG = "CreateTaskViewModel"
    }

    fun createTask(task: Task, boardListInfo: BoardListInfo, onSuccessCallback: () -> Unit) =
        viewModelScope.launch {
            _isSavingTask.value = true
            when (val result = firestoreRepository.createTask(
                task = task,
                listId = boardListInfo.id,
                listPath = boardListInfo.path
            )) {
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
