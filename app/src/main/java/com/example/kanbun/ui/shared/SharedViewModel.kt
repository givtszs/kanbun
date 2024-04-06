package com.example.kanbun.ui.shared

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.common.Result
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
  private val firestoreRepository: FirestoreRepository
) : ViewModel() {
    companion object {
        private const val TAG = "SharedViewModel"
    }

    private var _boardMembers: List<User> = emptyList()
    val boardMembers: List<User> get() = _boardMembers

    fun getBoardMembers(memberIds: List<String>) {
        viewModelScope.launch {
            Log.d(TAG, "Fetching board members")
            _boardMembers = when (val result = firestoreRepository.getMembers(memberIds)) {
                is Result.Success -> result.data
                is Result.Error -> emptyList()
            }
        }
    }
}