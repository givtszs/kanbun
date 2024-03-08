package com.example.kanbun.ui.board.board_list

import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Task

interface
BoardListViewHolderCallbacks {
    fun createTask(position: Int)
    fun onTaskClicked(task: Task, boardList: BoardList)

    fun openMenu(position: Int)
}