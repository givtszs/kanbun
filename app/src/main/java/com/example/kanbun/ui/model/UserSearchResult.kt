package com.example.kanbun.ui.model

import com.example.kanbun.domain.model.User

data class UserSearchResult(
    val user: User,
    val isAdded: Boolean
)