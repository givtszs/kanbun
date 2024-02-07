package com.example.kanbun.ui.create_task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.ui.BaseViewModel
import com.example.kanbun.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) : BaseViewModel() {
    private var _createTaskState = MutableStateFlow(ViewState.CreateTaskViewState())
    val createTaskState: StateFlow<ViewState.CreateTaskViewState> = _createTaskState

    suspend fun createTask(task: Task) {
        firestoreRepository.createTask(
            task = task,
            listId = task.boardListInfo.id,
            listPath = task.boardListInfo.path
        )
    }
}