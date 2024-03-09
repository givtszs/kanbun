package com.example.kanbun.ui.board.board_list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardListViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    fun editBoardListName(
        newName: String,
        boardListPath: String,
        boardListId: String,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        when (val result = firestoreRepository.updateBoardListName(newName, boardListPath, boardListId)) {
            is Result.Success -> onSuccess()
            is Result.Error -> Log.d("BoardListViewModel", "${result.message}")
            Result.Loading -> {}
        }

    }

    fun deleteBoardList(
        boardList: BoardList,
        boardLists: List<BoardList>,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        when (firestoreRepository.deleteBoardListAndRearrange(
            id = boardList.id,
            path = boardList.path,
            boardLists = boardLists,
            deleteAt = boardList.position.toInt()
        )) {
            is Result.Success -> onSuccess()
            is Result.Error -> {}
            Result.Loading -> {}
        }

    }
}