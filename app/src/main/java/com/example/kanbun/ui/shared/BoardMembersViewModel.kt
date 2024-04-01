package com.example.kanbun.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardMembersViewModel @Inject constructor(
  private val firestoreRepository: FirestoreRepository
) : ViewModel() {
    var boardMembers: List<User> = emptyList()

    fun getBoardMembers(memberIds: List<String>) {
        viewModelScope.launch {
            boardMembers = when (val result = firestoreRepository.getMembers(memberIds)) {
                is Result.Success -> result.data
                is Result.Error -> emptyList()
            }
        }
    }
}