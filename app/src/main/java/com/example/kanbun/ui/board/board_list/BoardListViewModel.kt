package com.example.kanbun.ui.board.board_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardListViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    fun editBoardListName(newName: String, boardListPath: String, boardListId: String) =
        viewModelScope.launch {
            firestoreRepository.updateBoardListName(newName, boardListPath, boardListId)
        }

    fun deleteBoardList(
        boardListPath: String,
        boardListId: String,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        when (firestoreRepository.deleteBoardList(boardListPath, boardListId)) {
            is Result.Success -> onSuccess()
            is Result.Error -> {}
            Result.Loading -> {}
        }

    }
}