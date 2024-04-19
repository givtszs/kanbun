package com.example.kanbun.ui.board.task_list

import com.example.kanbun.domain.model.TaskList
import com.example.kanbun.domain.model.Task

interface TaskListsAdapterCallbacks {
    fun createTaskList()

    fun onTaskListMenuClicked(taskList: TaskList, taskLists: List<TaskList>, isEnabled: Boolean)

    fun createTask(taskList: TaskList)

    fun onTaskClicked(task: Task, taskList: TaskList)

    fun loadingComplete()
}