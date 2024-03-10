package com.example.kanbun.ui.board_settings.edit_tags

import androidx.lifecycle.ViewModel
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.ui.ViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class EditTagsViewModel : ViewModel() {
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

    fun upsertTag(tag: Tag) {
        val isTagUpdate = _editTagsState.value.tags.any { it.id == tag.id }
        if (isTagUpdate) {
            // update the list of tags
            setTags(_editTagsState.value.tags.filterNot { _tag -> _tag.id == tag.id } + tag)
        } else {
            val doesTagExist = _editTagsState.value.tags.any { it.name == tag.name }
            if (!doesTagExist) {
                _editTagsState.update {
                    it.copy(tags = _editTagsState.value.tags + tag.copy(id = UUID.randomUUID().toString()))
                }
            } else {
                _editTagsState.update { it.copy(message = "Tag with the same name is already created") }
            }
        }
    }
}