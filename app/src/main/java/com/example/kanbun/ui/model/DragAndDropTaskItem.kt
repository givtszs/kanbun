package com.example.kanbun.ui.model

import com.example.kanbun.domain.model.TaskListInfo
import com.example.kanbun.domain.model.Task
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DragAndDropTaskItem(
    val task: Task,
    val initPosition: Int,
    val initAdapter: String,
    val initTaskList: TaskListInfo,
    val initTasksList: MutableList<Task>
)
