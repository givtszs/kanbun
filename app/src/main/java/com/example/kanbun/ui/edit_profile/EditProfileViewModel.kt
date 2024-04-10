package com.example.kanbun.ui.edit_profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.usecase.GetUserUseCase
import com.example.kanbun.domain.usecase.UpdateUserUseCase
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
    private val getUserUseCase: GetUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase
) : ViewModel() {

    private var _user = MutableStateFlow<User?>(null)
    private var _message = MutableStateFlow<String?>(null)
    private var _isLoading = MutableStateFlow(true)
    private var _nameError = MutableStateFlow<String?>(null)
    private var _tagError = MutableStateFlow<String?>(null)

    val editProfileState = combine(
        _user, _message, _isLoading, _nameError, _tagError
    ) { user, message, isLoading, nameError, tagError ->
        ViewState.EditProfileViewState(
            user = user,
            message = message,
            isLoading = isLoading,
            nameError = nameError,
            tagError = tagError
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

    fun updateUser(name: String, tag: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (_user.value == null) {
                _message.value = "Couldn't update the user since its value is null"
                return@launch
            }

            updateUserUseCase(
                oldUser = _user.value!!,
                newUser = _user.value!!.copy(
                    name = name,
                    tag = tag
                )
            )
                .onSuccess {
                    onSuccess()
                }
                .onError { message, _ ->
                    processError(message)
                }
        }
    }

    private fun processError(message: String?) {
        if (message == null) {
            return
        }

        val messageLowercase = message.lowercase()
        when {
            messageLowercase.contains("name") -> _nameError.value = message
            messageLowercase.contains("tag") ->  _tagError.value = message
            else -> _message.value = message
        }
    }

    fun messageShown() {
        _message.value = null
    }

    fun nameErrorShown() {
        _nameError.value = null
    }


    fun tagErrorShown() {
        _tagError.value = null
    }
}