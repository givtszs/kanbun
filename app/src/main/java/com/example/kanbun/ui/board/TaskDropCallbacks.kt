package com.example.kanbun.ui.board

import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.model.DragAndDropTaskItem

/**
 * Interface definition for a callbacks invoked when a [Task] dragged item has been released
 */
interface TaskDropCallbacks {
    /**
     * Called when a dragged view is dropped in a new adapter with insertion intent.
     *
     * @param adapter the [RecyclerView.Adapter] the dragged view has been dropped into
     * @param dragItem the drag and drop data item
     * @param position the drop position of the dragged view in the [RecyclerView] list
     */
    fun dropToInsert(adapter: TasksAdapter, dragItem: DragAndDropTaskItem, position: Int)

    /**
     * Called when a dragged view is dropped in the hosting adapter with moving intent.
     *
     * @param adapter the [RecyclerView.Adapter] the dragged view has been dropped into
     * @param from the position to move from
     * @param to the position to move to
     */
    fun dropToMove(adapter: TasksAdapter, from: Int, to: Int)
}