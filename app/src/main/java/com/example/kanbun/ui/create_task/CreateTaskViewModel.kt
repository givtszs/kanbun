package com.example.kanbun.ui.create_task

import androidx.lifecycle.ViewModel
import com.example.kanbun.ui.ViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CreateTaskViewModel : ViewModel() {
    private var _createTaskState = MutableStateFlow(ViewState.CreateTaskViewState())
    val createTaskState: StateFlow<ViewState.CreateTaskViewState> = _createTaskState


}