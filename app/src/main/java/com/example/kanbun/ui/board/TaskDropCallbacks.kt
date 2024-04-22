package com.example.kanbun.ui.board

import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.TaskListInfo
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.model.DragAndDropTaskItem

/**
 * Interface definition for a callbacks invoked when a [Task] dragged item has been released
 */
interface TaskDropCallbacks {
    /**
     * Called when a dragged view is dropped in a new adapter with insertion intent.
     *
     * @param tasks the list of tasks to insert the task into.
     * @param taskListInfo the id and the Firestore path of the task list to insert the task into.
     * @param dragItem the drag and drop data item.
     * @param position the drop position of the dragged view in the [RecyclerView] list.
     */
    fun dropToInsert(tasks: List<Task>, taskListInfo: TaskListInfo, dragItem: DragAndDropTaskItem, position: Int)

    /**
     * Called when a dragged view is dropped in the hosting adapter with moving intent.
     *
     * @param tasks the list of tasks to insert the task into.
     * @param taskListInfo the id and the Firestore path of the task list to insert the task into.
     * @param from the position to move from.
     * @param to the position to move to.
     */
    fun dropToMove(tasks: List<Task>, taskListInfo: TaskListInfo, from: Int, to: Int)
}