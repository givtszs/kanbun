package com.example.kanbun.ui.board.board_list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.repository.TaskListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardListViewModel @Inject constructor(
    private val taskListRepository: TaskListRepository
) : ViewModel() {

    fun editBoardListName(
        newName: String,
        boardListPath: String,
        boardListId: String,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        when (val result = taskListRepository.updateTaskListName(newName, boardListPath, boardListId)) {
            is Result.Success -> onSuccess()
            is Result.Error -> Log.d("BoardListViewModel", "${result.message}")
        }

    }

    fun deleteBoardList(
        boardList: BoardList,
        boardLists: List<BoardList>,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        when (taskListRepository.deleteTaskListAndRearrange(
            id = boardList.id,
            path = boardList.path,
            taskLists = boardLists,
            deleteAt = boardList.position.toInt()
        )) {
            is Result.Success -> onSuccess()
            is Result.Error -> {}
        }

    }
}