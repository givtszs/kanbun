package com.example.kanbun.ui.board

import android.content.ClipData
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter

interface DropCallback {
    fun drop(clipData: ClipData, adapter: TasksAdapter, position: Int): Boolean
}