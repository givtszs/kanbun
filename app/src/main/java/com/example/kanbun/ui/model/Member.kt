package com.example.kanbun.ui.model

import com.example.kanbun.domain.model.User

data class Member<T>(
    val user: User,
    val role: T
)