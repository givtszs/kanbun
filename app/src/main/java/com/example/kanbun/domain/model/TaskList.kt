package com.example.kanbun.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskList(
    val id: String = "",
    val name: String = "",
    val position: Long = 0,
    val path: String = "",
    val tasks: List<Task> = emptyList()
) : Parcelable
