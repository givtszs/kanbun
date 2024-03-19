package com.example.kanbun.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// TODO: Rename to `TasksList`
@Parcelize
data class BoardList(
    val id: String = "",
    val name: String = "",
    val position: Long = 0,
    val path: String = "",
    val tasks: List<Task> = emptyList()
) : Parcelable
