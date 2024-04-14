package com.example.kanbun.ui.shared

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedBoardViewModel @Inject constructor(
  private val firestoreRepository: FirestoreRepository
) : ViewModel() {
    companion object {
        private const val TAG = "SharedBoardViewModel"
    }

    private var _boardMembers: List<User> = emptyList()
        set (value) {
            field = value
            boardMemberIds = value.map { it.id }
        }
    val boardMembers: List<User> get() = _boardMembers

    var boardMemberIds: List<String> = emptyList()
        private set

    private var fetchBoardsJob: Job? = null
    fun fetchBoardMembers(memberIds: List<String>) {
        fetchBoardsJob?.cancel()
        fetchBoardsJob = viewModelScope.launch {
            Log.d(TAG, "Fetching board members")
            _boardMembers = when (val result = firestoreRepository.getMembers(memberIds)) {
                is Result.Success -> {
                    Log.d(TAG, "Fetched board members: ${result.data}")
                    result.data
                }
                is Result.Error -> emptyList()
            }
        }
    }
}