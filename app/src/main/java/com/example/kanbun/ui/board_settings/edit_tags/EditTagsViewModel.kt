package com.example.kanbun.ui.board_settings.edit_tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.usecase.UpsertTagUseCase
import com.example.kanbun.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTagsViewModel @Inject constructor(
    private val createTagUseCase: UpsertTagUseCase,
) : ViewModel() {
    private var _editTagsState = MutableStateFlow(ViewState.EditTagsViewState())
    val editTagsState: StateFlow<ViewState.EditTagsViewState> = _editTagsState

    fun setTags(tags: List<Tag>) {
        _editTagsState.update {
            it.copy(tags = tags.sortedBy { tag -> tag.name })
        }
    }

    fun messageShown() {
        _editTagsState.update { it.copy(message = null) }
    }

    fun upsertTag(tag: Tag, board: Board) {
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
                    val isTagUpdate = _editTagsState.value.tags.any { it.id == tag.id }
                    if (isTagUpdate) {
                        // update the list of tags
                        setTags(_editTagsState.value.tags.filterNot { _tag -> _tag.id == tag.id } + tag)
                    } else {
                        _editTagsState.update {
                            it.copy(tags = _editTagsState.value.tags + result.data)
                        }
                    }

                }

                is Result.Error -> _editTagsState.update { it.copy(message = result.message) }
                Result.Loading -> {}
            }
        }
    }
}