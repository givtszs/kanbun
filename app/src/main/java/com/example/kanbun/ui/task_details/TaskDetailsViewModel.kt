package com.example.kanbun.ui.task_details


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.main_activity.MainActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: Add network connectivity checks to this and others view models.

private const val TAG = "TaskDetailsViewModel"

@HiltViewModel
class TaskDetailsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private var _taskDetailsState = MutableStateFlow(ViewState.TaskDetailsViewState())
    val taskDetailsState: StateFlow<ViewState.TaskDetailsViewState> = _taskDetailsState


    fun messageShown() {
        _taskDetailsState.update {
            it.copy(message = null)
        }
    }

    fun getAuthor(userId: String) {
        Log.d(TAG, "getAuthor is called")
        viewModelScope.launch {
            if (MainActivity.firebaseUser == null) {
                _taskDetailsState.update {
                    it.copy(message = "The user is null")
                }
                return@launch
            }

            when (val result = firestoreRepository.getUser(userId)) {
                is Result.Success -> _taskDetailsState.update {
                    it.copy(author = result.data)
                }
                is Result.Error -> _taskDetailsState.update {
                    it.copy(message = result.message)
                }
            }
        }
    }

    fun deleteTask(taskPosition: Int, boardList: BoardList, navigateOnDelete: () -> Unit) =
        viewModelScope.launch {
            when (
                val result = firestoreRepository.deleteTaskAndRearrange(
                    listPath = boardList.path,
                    listId = boardList.id,
                    tasks = boardList.tasks,
                    from = taskPosition
                )
            ) {
                is Result.Success -> navigateOnDelete()
                is Result.Error -> _taskDetailsState.update {
                    it.copy(message = result.message)
                }
            }
        }
}