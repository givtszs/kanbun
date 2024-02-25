package com.example.kanbun.ui.board_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.ui.BaseViewModel
import com.example.kanbun.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardSettingsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) : BaseViewModel() {
    private var _boardSettingsState = MutableStateFlow(ViewState.BoardSettingsViewState())
    val boardSettingsState: StateFlow<ViewState.BoardSettingsViewState> = _boardSettingsState

    fun deleteBoard(boardId: String, workspaceId: String, onSuccess: () -> Unit) =
        viewModelScope.launch {
            processResult(firestoreRepository.deleteBoard(boardId, workspaceId), onSuccess)
        }

    fun updateBoard(board: Board, onSuccess: () -> Unit) = viewModelScope.launch {
        processResult(firestoreRepository.updateBoard(board), onSuccess)
    }

    private fun <T : Any> processResult(result: Result<T>, onSuccess: () -> Unit) {
        when (result) {
            is Result.Success -> onSuccess()
            is Result.Error -> _boardSettingsState.update {
                it.copy(message = result.message)
            }

            Result.Loading -> {}
        }
    }

    fun messageShown() {
        _boardSettingsState.update {
            it.copy(message = null)
        }
    }
}