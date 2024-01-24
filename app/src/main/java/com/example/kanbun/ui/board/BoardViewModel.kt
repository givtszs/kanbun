package com.example.kanbun.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.ui.ViewState.BoardViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BoardViewModel(
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _board = MutableStateFlow(Board())
    private var _boardLists: Flow<List<BoardList>> = flowOf(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    val boardState: StateFlow<BoardViewState> =
        combine(_board, _boardLists, _isLoading, _message) { board, boardLists, isLoading, message ->
            BoardViewState(
                board = board,
                lists = boardLists,
                isLoading = isLoading,
                message = message
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), BoardViewState())

    fun initBoard(boardId: String, workspaceId: String) = viewModelScope.launch {
        // get board
        when (val result = firestoreRepository.getBoard(workspaceId, boardId)) {
            is Result.Success -> _board.value = result.data
            is Result.Error -> _message.value = result.message
        }

        _boardLists = firestoreRepository.getBoardListsFlow(_board.value)
    }
}