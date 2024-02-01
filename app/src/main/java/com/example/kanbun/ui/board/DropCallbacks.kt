package com.example.kanbun.ui.board

import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.model.DragAndDropTaskItem

interface DropCallbacks {
    fun dropToInsert(adapterToInsert: TasksAdapter, dragItem: DragAndDropTaskItem, to: Int)

    fun dropToMove(adapter: TasksAdapter, from: Int, to: Int)
}