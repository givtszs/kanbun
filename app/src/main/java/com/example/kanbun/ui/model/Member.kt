package com.example.kanbun.ui.model

import com.example.kanbun.common.Role
import com.example.kanbun.domain.model.User

data class Member(
    val user: User,
    val role: Role
)