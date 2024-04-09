package com.example.kanbun.ui.edit_profile

import androidx.browser.customtabs.CustomTabsIntent.ShareState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.domain.usecase.GetUserUseCase
import com.example.kanbun.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {

    private var _message = MutableStateFlow<String?>(null)
    private var _isLoading = MutableStateFlow(true)
    private var _user = MutableStateFlow<User?>(null)

    val editProfileState = combine(
        _message, _isLoading, _user
    ) { message, isLoading, user ->
        ViewState.EditProfileViewState(
            message = message,
            isLoading = isLoading,
            user = user
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        ViewState.EditProfileViewState()
    )

    init {
        getUserData()
    }

    private fun getUserData() {
        viewModelScope.launch {
            getUserUseCase()
                .onSuccess { user ->
                    _user.value = user
                }
                .onError { message, _ ->
                    _message.value = message
                }
            _isLoading.value = false
        }
    }

}