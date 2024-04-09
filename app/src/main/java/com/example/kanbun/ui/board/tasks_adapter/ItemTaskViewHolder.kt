package com.example.kanbun.ui.board.tasks_adapter

import android.content.ClipData
import android.content.ClipDescription
import android.util.Log
import android.view.DragEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.DATE_TIME_FORMAT
import com.example.kanbun.common.convertTimestampToDateString
import com.example.kanbun.common.moshi
import com.example.kanbun.databinding.ItemTaskBinding
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.board.TaskDropCallbacks
import com.example.kanbun.ui.board.tasks_adapter.ItemTaskViewHolder.TaskDragAndDropHelper.DROP_ZONE_TASK
import com.example.kanbun.ui.model.DragAndDropTaskItem
import com.example.kanbun.ui.custom_views.TagView
import com.squareup.moshi.Moshi

class ItemTaskViewHolder(
    private val binding: ItemTaskBinding,
    private val tasksAdapter: TasksAdapter,
    isWorkspaceAdminOrBoardMember: Boolean,
    private val clickAtPosition: (Int) -> Unit,
    private val loadTags: (List<String>) -> List<Tag>
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        var draggedTaskInitPosition = -1
        var draggedTaskPrevPosition = -1
        private const val TAG = "ItemTaskViewHolder"
    }

    private var task: Task? = null

    init {
        binding.materialCard.setOnClickListener {
            clickAtPosition(adapterPosition)
        }

        if (isWorkspaceAdminOrBoardMember) {
            // initiates dragging action
            binding.materialCard.setOnLongClickListener { view ->
                Log.d(TAG, "long clicked perform at: $adapterPosition")
                draggedTaskInitPosition = adapterPosition
                draggedTaskPrevPosition = adapterPosition

                TaskDragAndDropHelper.startDrag(tasksAdapter, view, task)
            }
        }

        binding.materialCard.setOnDragListener { receiverView, event ->
            TaskDragAndDropHelper.taskCardViewDragEventHandler(
                tasksAdapter,
                receiverView,
                event,
                adapterPosition
            )
        }

        binding.root.setOnDragListener { _, event ->
            TaskDragAndDropHelper.taskRootViewDragEventHandler(event)
        }
    }

    fun bind(task: Task) {
        Log.d("TasksAdapter", "bind:\ttask: $task")
        this.task = task

        binding.apply {
            materialCard.visibility = if (task.id != DROP_ZONE_TASK) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }

            setUpTagsFlexbox(task)

            tvName.text = task.name
            tvName.layoutParams = (tvName.layoutParams as ConstraintLayout.LayoutParams).apply {
                topMargin = if (flexTags.visibility == View.GONE) {
                    0
                } else {
                    flexTags.resources.getDimensionPixelSize(R.dimen.task_name_top_margin)
                }
            }

            tvDescription.isVisible = task.description.isNotEmpty()
            tvDescription.text = task.description

            // set up date
            getDisplayDate(task.dateStarts, task.dateEnds).also { date ->
                tvDate.text = date
                tvDate.isVisible = date != null
                separatorHorizontal.isVisible = date != null
            }
        }
    }

    private fun getDisplayDate(dateStarts: Long?, dateEnds: Long?): String? {
        return when {
            dateStarts != null && dateEnds != null ->
                itemView.resources.getString(
                    R.string.task_date,
                    convertTimestampToDateString(DATE_TIME_FORMAT, dateStarts),
                    convertTimestampToDateString(DATE_TIME_FORMAT, dateEnds)
                )

            dateStarts != null ->
                itemView.resources.getString(
                    R.string.date_starts,
                    convertTimestampToDateString(
                        DATE_TIME_FORMAT,
                        dateStarts
                    )
                )

            dateEnds != null ->
                itemView.resources.getString(
                R.string.date_ends,
                convertTimestampToDateString(
                    DATE_TIME_FORMAT,
                    dateEnds
                )
            )

            else -> null
        }
    }

    private fun setUpTagsFlexbox(task: Task) {
        val tags = loadTags(task.tags)
        Log.d(TAG, "tags: $tags")

        if (tags.isEmpty()) {
            binding.flexTags.isVisible = false
            return
        }

        binding.apply {
            flexTags.isVisible = true
            flexTags.removeAllViews()

            tags.forEach {  tag ->
                val tagView = TagView(context = itemView.context, isBig = false).also {
                    it.bind(tag, isClickable = false, isSelected = false)
                }
                flexTags.addView(tagView)
            }
        }
    }

    /**
     * A helper singleton class for managing drag-and-drop interactions and states within the [TasksAdapter]
     */
    private object TaskDragAndDropHelper {
        const val DROP_ZONE_TASK = "drop_zone"

        /** The dragged task's hosting [TasksAdapter] */
        private lateinit var taskInitAdapter: TasksAdapter

        /** The [TasksAdapter] the dragged task is currently interacting with */
        private var currentAdapter: TasksAdapter? = null

        /** Indicates whether the end of the drag and drop has been handled */
        private var isActionDragEndedHandled = false

        /**
         * Indicates the `drop zone` acting as the empty space between tasks.
         *
         * The ViewHolder item for this task must be invisible.
         */
        private var dropZoneTask = Task(id = DROP_ZONE_TASK)

        /** Stores the dragged task removed from the hosting adapter.
         *
         * Use it to restore the dragged task if the drag and drop action failed.
         */
        private var tempRemovedTask: Task? = null

        /** Indicates whether the user has dragged the view over another adapter */
        private var isNewAdapter = false

        fun startDrag(adapter: TasksAdapter, view: View, task: Task?): Boolean {
            taskInitAdapter = adapter
            currentAdapter = adapter
            isActionDragEndedHandled = false
            isNewAdapter = false

            val dragData = prepareDragData(adapter, task) ?: return false
            val taskShadow = View.DragShadowBuilder(view)

            val isSuccess = view.startDragAndDrop(dragData, taskShadow, view, 0)
            if (isSuccess) {
                view.visibility = View.INVISIBLE
            }

            return isSuccess
        }

        private fun prepareDragData(adapter: TasksAdapter, task: Task?): ClipData? {
            val json = moshi.adapter(DragAndDropTaskItem::class.java).toJson(
                DragAndDropTaskItem(
                    task = task ?: return null,
                    initPosition = draggedTaskInitPosition,
                    initAdapter = adapter.toString(),
                    initBoardList = adapter.listInfo,
                    initTasksList = adapter.tasks
                )
            )
            Log.d(TAG, "Parsed task: $json")

            val item = ClipData.Item(json)
            return ClipData(
                "task_json",
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item
            )
        }

        // flags indicating the movement state of the draggable item over a task's drag listener
        private var isMovedUp = false
        private var isMovedDown = false
        private var isInsertHandled = false

        fun taskCardViewDragEventHandler(
            adapter: TasksAdapter,
            receiverView: View,
            event: DragEvent,
            position: Int
        ): Boolean {
            return when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    Log.d(TAG, "task card: ACTION_DRAG_STARTED")
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }

                DragEvent.ACTION_DRAG_ENTERED -> {
                    Log.d(
                        TAG,
                        "ACTION_DRAG_ENTERED: View under drag: $position\n" +
                                "currAdapter: $adapter,\n" +
                                "lastAdapter: $currentAdapter"
                    )

                    // check if the dragged item has entered an adapter other than hosting it
                    isNewAdapter = currentAdapter != adapter

                    if (isNewAdapter) {
                        // if the dragged item has entered a new adapter, remove either
                        // the previously created drop zone, or the dragged item, from the dataset
                        val isDropZoneRemoved = removeDropZone()
                        if (!isDropZoneRemoved) {
                            removeDataAt(currentAdapter!!, draggedTaskInitPosition)
                        }
                        currentAdapter = adapter
                    }
                    true
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    val pivot = receiverView.height / 2

                    if (!isNewAdapter) {
                        if (event.y < pivot && !isMovedUp) {
                            isMovedDown = false
                            isMovedUp = true
                            val newPos =
                                if (position < draggedTaskPrevPosition) position else position - 1
                            Log.d(
                                TAG,
                                "ACTION_DRAG_LOCATION: oldPos: $draggedTaskPrevPosition, newPos: $newPos"
                            )
                            moveTask(draggedTaskPrevPosition, newPos)
                        } else if (event.y > pivot && !isMovedDown) {
                            isMovedUp = false
                            isMovedDown = true
                            val newPos =
                                if (position < draggedTaskPrevPosition) position + 1 else position
                            Log.d(
                                TAG,
                                "ACTION_DRAG_LOCATION: oldPos: $draggedTaskPrevPosition, newPos: $newPos"
                            )
                            moveTask(draggedTaskPrevPosition, newPos)
                        }
                    } else if (!isInsertHandled) {
                        isInsertHandled = true
                        Log.d(
                            TAG,
                            "ACTION_DRAG_LOCATION: insertPos: $position"
                        )
                        insertDropZone(adapter, position)
                    }

                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    isMovedUp = false
                    isMovedDown = false
                    isInsertHandled = false
                    true
                }

                DragEvent.ACTION_DROP -> {
                    Log.d(TAG, "ACTION_DROP")
                    // why not to use the currentWorkingAdapter
//                    adapter.dragAndDropHelper.drop(
//                        clipData = event.clipData,
//                        adapter.taskDropCallbacks
//                    )

                    handleDrop(clipData = event.clipData, adapter.taskDropCallbacks)
                }

                else -> false
            }
        }

        /**
         * Drag event handler for the root view of the task's materical card view.
         */
        fun taskRootViewDragEventHandler(event: DragEvent): Boolean {
            val draggableView = event.localState as View
            return when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }

                DragEvent.ACTION_DROP -> {
                    Log.d(TAG, "Root#ACTION_DROP at $this")
                    handleDrop(
                        event.clipData,
                        taskInitAdapter.taskDropCallbacks
                    )
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    if (!isActionDragEndedHandled) {
                        Log.d(TAG, "Root#ACTION_DRAG_ENDED: handle action")
                        draggableView.visibility = View.VISIBLE

                        // if the drag and drop failed
                        Log.d(
                            TAG, "Root#ACTION_DRAG_ENDED: event result: ${event.result}\n" +
                                    "tempRemovedTask: $tempRemovedTask"
                        )

                        // if the drag-and-drop failed...
                        if (!event.result) {
                            with(taskInitAdapter) {
                                // ...and the dragged task was deleted from the initial list in case of
                                // being dragged out of it to be inserted in another list...
                                tempRemovedTask?.let { task ->
                                    // ...remove the created drop zone in a new list...
                                    removeDropZone()
                                    if (task.id != DROP_ZONE_TASK) {
                                        Log.d(
                                            TAG,
                                            "Root#ACTION_DRAG_ENDED: bring back draggable item"
                                        )
                                        // ...and restore the task in the initial list
                                        addData(task.position.toInt(), task)
                                        tempRemovedTask = null
                                    }
                                }

                                notifyDataSetChanged()
                            }
                        }

                        isActionDragEndedHandled = true
                    }

                    true
                }

                else -> false
            }
        }

        /**
         * Moves the task item in the [RecyclerView] list.
         *
         * @param from the position to move from
         * @param to the position to move to
         */
        private fun moveTask(from: Int, to: Int) {
            if (from != to && to != -1) {
                currentAdapter?.notifyItemMoved(from, to)
                draggedTaskPrevPosition = to
                Log.d(TAG, "move: Moved item from $from to $to")
            }
        }

        /**
         * Removes the drop zone from the [RecyclerView] and [TasksAdapter] datasets
         */
        private fun removeDropZone(): Boolean {
            val isDropZoneTaskPresent = currentAdapter?.tasks?.any { it.id == DROP_ZONE_TASK }
            return if (isDropZoneTaskPresent == true) {
                // when we move items only the underlying dataset gets updated,
                // so in the tasks list we remove dragShadowTask at its insert position...
                currentAdapter?.tasks?.removeAt(dropZoneTask.position.toInt())
                dropZoneTask = dropZoneTask.copy(position = -1)
                // ...but in the underlying dataset we remove item at the last moved position
                currentAdapter?.notifyItemRemoved(draggedTaskPrevPosition)
                Log.d(
                    TAG,
                    "removeDropZone: removed drop zone at position $draggedTaskPrevPosition"
                )
                true
            } else {
                false
            }
        }

        /**
         * Removes an item at the [position] from the [TasksAdapter] dataset.
         *
         * Use if the item needs to be inserted into another adapter
         *
         * @param position the position to remove the item at
         */
        private fun removeDataAt(adapter: TasksAdapter, position: Int) {
            if (position != -1) {
                tempRemovedTask = adapter.tasks[position]
                Log.d(
                    TAG,
                    "removeDataAt: tempRemovedTask: $tempRemovedTask"
                )
                // when we move items only the underlying dataset gets updated,
                // so in the tasks list we remove item at its insert position...
                adapter.tasks.removeAt(position)
                // ...but in the underlying dataset we remove item at the last moved position
                adapter.notifyItemRemoved(draggedTaskPrevPosition)
                draggedTaskPrevPosition = -1 // TODO: Check if this reset is vital
                Log.d(TAG, "removeData: removed item at $position")
            }
        }

        /**
         * Creates a drop zone in the [adapter] at the given [position].
         *
         * The drop zone is basically the empty space between the adapter's items acting as an indicator
         * where the dragged item will be located if dropped
         *
         * @param adapter [TasksAdapter] to create a drop zone into
         * @param position the position to create a drop zone at
         */
        private fun insertDropZone(adapter: TasksAdapter, position: Int) {
            if (!adapter.tasks.any { it.id == DROP_ZONE_TASK }) {
                dropZoneTask = dropZoneTask.copy(position = position.toLong())
                adapter.tasks.add(position, dropZoneTask)
                adapter.notifyItemInserted(position)
                draggedTaskPrevPosition = position
                Log.d(TAG, "insertDropZone: inserted drop_zone")
            }
        }

        /**
         * Called when the dragged item has been dropped.
         *
         * If the dragged item has been dropped in the hosting adapter, i.e., the adapter the item
         * was initially stored into, the [TaskDropCallbacks.dropToMove] callback is called.
         *
         * If the dragged item has been dropped in an adapter other than the hosting, the [TaskDropCallbacks.dropToInsert]
         * callback is called.
         */
        private fun handleDrop(clipData: ClipData, taskDropCallbacks: TaskDropCallbacks): Boolean {
            val data = clipData.getItemAt(0).text.toString()
            val moshi = Moshi.Builder().build()
            val jsonAdapter = moshi.adapter(DragAndDropTaskItem::class.java)
            val dragItem = jsonAdapter.fromJson(data)

            return if (dragItem == null) {
                Log.d(
                    TAG,
                    "drop: dragItem is null"
                )
                false
            } else {
                val containedDropZone = removeDropZone()
                Log.d(
                    TAG,
                    "drop: isNewAdapter: ${isNewAdapter}, " +
                            "containedDropZone: $containedDropZone"
                )

                // TODO: inspect whether this double checks is required, or isNewAdapter check is enough
                if (isNewAdapter || containedDropZone) {
                    Log.d(
                        TAG,
                        "drop: insert task ${dragItem.task}"
                    )

                    taskDropCallbacks.dropToInsert(
                        currentAdapter!!,
                        dragItem,
                        draggedTaskPrevPosition
                    )
                    tempRemovedTask = null
                } else {
                    Log.d(
                        TAG,
                        "drop: move tasks from ${dragItem.initPosition} to $draggedTaskPrevPosition"
                    )
                    taskDropCallbacks.dropToMove(
                        currentAdapter!!,
                        dragItem.initPosition,
                        draggedTaskPrevPosition
                    )
                }
                true
            }
        }
    }
}