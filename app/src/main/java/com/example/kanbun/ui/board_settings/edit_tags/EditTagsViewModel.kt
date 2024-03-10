package com.example.kanbun.ui.board_settings.edit_tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.domain.usecase.CreateTagUseCase
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.model.TagUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTagsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val createTagUseCase: CreateTagUseCase,
) : ViewModel() {
    private var _editTagsState = MutableStateFlow(ViewState.EditTagsViewState())
    val editTagsState: StateFlow<ViewState.EditTagsViewState> = _editTagsState

    fun setTags(tags: List<Tag>) {
        _editTagsState.update {
            it.copy(tags = tags.map { tag -> TagUi(tag, false) })
        }
    }

    fun createTag(tag: Tag, board: Board) {
        viewModelScope.launch {
            when (
                val result = createTagUseCase(
                    tag = tag,
                    tags = _editTagsState.value.tags,
                    boardPath = "${FirestoreCollection.WORKSPACES.collectionName}/${board.workspace.id}" +
                            "/${FirestoreCollection.BOARDS.collectionName}",
                    boardId = board.id,
                )
            ) {
                is Result.Success -> {
                    _editTagsState.update {
                        it.copy(tags = _editTagsState.value.tags + TagUi(tag, false))
                    }
                }

                is Result.Error -> _editTagsState.update { it.copy(message = result.message) }
                Result.Loading -> {}
            }
        }
    }
}