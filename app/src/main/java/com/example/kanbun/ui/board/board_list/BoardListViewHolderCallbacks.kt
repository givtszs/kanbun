package com.example.kanbun.ui.board.board_list

import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Task

interface BoardListViewHolderCallbacks {
    fun createTask(position: Int)
    fun onTaskClicked(task: Task, boardListInfo: BoardListInfo)

    fun openMenu(position: Int)
}