package com.example.kanbun.ui.board.lists_adapter

import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Task

interface BoardListsAdapterCallbacks {
    fun createBoardList()

    fun onBoardListMenuClicked(boardList: BoardList)

    fun createTask(boardList: BoardList)

    fun onTaskClicked(task: Task, boardListInfo: BoardListInfo)

    fun loadingComplete()
}