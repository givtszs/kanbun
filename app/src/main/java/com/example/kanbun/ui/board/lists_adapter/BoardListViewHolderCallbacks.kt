package com.example.kanbun.ui.board.lists_adapter

import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.board.TaskDropCallbacks

interface BoardListViewHolderCallbacks {
    fun onCreateTask(position: Int)
    fun onTaskClicked(task: Task, boardListInfo: BoardListInfo)

    fun onMenuClicked()
}