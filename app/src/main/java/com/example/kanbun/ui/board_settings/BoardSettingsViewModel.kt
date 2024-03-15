package com.example.kanbun.ui.board_settings

import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.domain.usecase.UpsertTagUseCase
import com.example.kanbun.ui.BaseViewModel
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.model.TagUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardSettingsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val createTagUseCase: UpsertTagUseCase
) : BaseViewModel() {
    private var _boardSettingsState = MutableStateFlow(ViewState.BoardSettingsViewState())
    val boardSettingsState: StateFlow<ViewState.BoardSettingsViewState> = _boardSettingsState

    fun init(tags: List<Tag>) {
        setTags(tags)
    }

    fun deleteBoard(board: Board, onSuccess: () -> Unit) =
        viewModelScope.launch {
            _boardSettingsState.update { it.copy(isLoading = true) }
            processResult(firestoreRepository.deleteBoard(board), onSuccess)
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

    fun setTags(tags: List<Tag>) {
        if (!areTagsSame(
            old = _boardSettingsState.value.tags.map { it.tag },
            new = tags
        )) {
            _boardSettingsState.update {
                it.copy(
                    tags = tags.map { tag -> TagUi(tag, false) }
                )
            }
        }
    }
    
    private fun areTagsSame(old: List<Tag>, new: List<Tag>): Boolean = old == new
}