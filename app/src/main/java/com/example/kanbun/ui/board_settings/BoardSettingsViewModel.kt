package com.example.kanbun.ui.board_settings

import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.domain.usecase.UpdateBoardUseCase
import com.example.kanbun.domain.usecase.UpsertTagUseCase
import com.example.kanbun.ui.BaseViewModel
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.model.TagUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardSettingsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val createTagUseCase: UpsertTagUseCase,
    private val updateBoardUseCase: UpdateBoardUseCase
) : BaseViewModel() {
    private var _tags = MutableStateFlow<List<Tag>>(emptyList())
    private var _isLoading = MutableStateFlow(false)
    private var _message = MutableStateFlow<String?>(null)
    val boardSettingsState: StateFlow<ViewState.BoardSettingsViewState> =
        combine(_tags, _isLoading, _message) { tags, isLoading, message ->
            ViewState.BoardSettingsViewState(
                tags = tags.map { TagUi(it, false) },
                isLoading = isLoading,
                message = message
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            ViewState.BoardSettingsViewState()
        )

    fun init(tags: List<Tag>) {
        setTags(tags)
    }

    fun deleteBoard(board: Board, onSuccess: () -> Unit) =
        viewModelScope.launch {
            _isLoading.value = true
            processResult(firestoreRepository.deleteBoard(board), onSuccess)
        }

    fun updateBoard(oldBoard: Board, newBoard: Board, onSuccess: () -> Unit) =
        viewModelScope.launch {
            processResult(updateBoardUseCase(oldBoard, newBoard), onSuccess)
        }

    private fun <T : Any> processResult(result: Result<T>, onSuccess: () -> Unit) {
        when (result) {
            is Result.Success -> onSuccess()
            is Result.Error -> _message.value = result.message
            Result.Loading -> {}
        }
    }

    fun messageShown() {
        _message.value = null
    }

    fun setTags(tags: List<Tag>) {
        if ( _tags.value != tags) {
            _tags.value = tags
        }
    }
}