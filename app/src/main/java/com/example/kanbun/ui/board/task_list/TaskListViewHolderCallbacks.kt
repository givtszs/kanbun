package com.example.kanbun.ui.board.task_list

import com.example.kanbun.domain.model.TaskList
import com.example.kanbun.domain.model.Task

interface
TaskListViewHolderCallbacks {
    fun createTask(position: Int)
    fun onTaskClicked(task: Task, taskList: TaskList)

    fun openMenu(position: Int)
}