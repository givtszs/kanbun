package com.example.kanbun.ui.board

import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter

interface DropCallbacks {
    fun dropToInsert(adapter: TasksAdapter, tasksToRemoveFrom: List<Task>, task: Task, from: Int, to: Int)

    fun dropToMove(adapter: TasksAdapter, from: Int, to: Int)
}