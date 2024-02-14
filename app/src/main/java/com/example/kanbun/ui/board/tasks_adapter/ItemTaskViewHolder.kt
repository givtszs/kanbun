package com.example.kanbun.ui.board.tasks_adapter

import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Color
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.moshi
import com.example.kanbun.databinding.ItemTaskBinding
import com.example.kanbun.databinding.ItemTaskTagBinding
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.board.TaskDropCallbacks
import com.example.kanbun.ui.board.tasks_adapter.ItemTaskViewHolder.TaskDragAndDropHelper.Companion.DROP_ZONE_TASK
import com.example.kanbun.ui.model.DragAndDropTaskItem
import com.squareup.moshi.Moshi

class ItemTaskViewHolder(
    private val binding: ItemTaskBinding,
    private val boardTags: List<Tag>,
    private val tasksAdapter: TasksAdapter,
    private val clickAtPosition: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        var draggedTaskInitPosition = -1
        var draggedTaskPrevPosition = -1
    }

    private var task: Task? = null

    private val dragAndDropHelper = TaskDragAndDropHelper(tasksAdapter)

    init {
        binding.materialCard.setOnClickListener {
            clickAtPosition(adapterPosition)
        }

        // initiates dragging action
        binding.materialCard.setOnLongClickListener { view ->
            Log.d("ItemTaskViewHolder", "long clicked perfrom at: $adapterPosition")
            draggedTaskInitPosition = adapterPosition
            draggedTaskPrevPosition = adapterPosition

            dragAndDropHelper.startDrag(view, task)
        }

        binding.materialCard.setOnDragListener { receiverView, event ->
            val draggableView = event?.localState as View
            dragAndDropHelper.handleDragEvent(receiverView, draggableView, event, adapterPosition)
        }
    }

    fun bind(task: Task) {
        Log.d("TasksAdapter", "bind:\ttask: $task")
        this.task = task

        binding.apply {
            materialCard.visibility =
                if (task.id != DROP_ZONE_TASK) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }

            with(tvName) {
                text = task.name
                val tvNameParams = (layoutParams as ConstraintLayout.LayoutParams).apply {
                    topMargin = if (flexTags.visibility == View.GONE) {
                        0
                    } else {
                        flexTags.resources.getDimensionPixelSize(R.dimen.task_name_top_margin)
                    }
                }
                layoutParams = tvNameParams
            }

            // set up tags flexbox layout
            with(flexTags) {
                removeAllViews()
                val verticalPadding = resources.getDimensionPixelSize(R.dimen.tags_vertical_padding)

                if (boardTags.isNotEmpty()) {
                    boardTags.onEach { tag ->
                        if (tag.id in task.tags) {
                            val tagBinding = ItemTaskTagBinding.inflate(
                                LayoutInflater.from(itemView.context),
                                this@with,
                                false
                            ).apply {
                                cardTag.isClickable = false
                                tvTag.text = tag.name
                                tvTag.setTextColor(Color.parseColor(tag.textColor))
                                cardTag.setCardBackgroundColor(Color.parseColor(tag.backgroundColor))
                                root.setPadding(0, verticalPadding, 0, verticalPadding)
                            }
                            addView(tagBinding.root)
                        }
                    }
                }
            }
        }
    }

    /**
     * A helper class for managing drag-and-drop interactions and states within the [TasksAdapter]
     *
     * @property adapter the [TasksAdapter] instance associated with this drag-and-drop helper
     */
    class TaskDragAndDropHelper(private val adapter: TasksAdapter) {
        companion object {
            const val DROP_ZONE_TASK = "drop_zone"

            /** The dragged task's hosting [TasksAdapter] */
            lateinit var taskInitAdapter: TasksAdapter

            /** The [TasksAdapter] the dragged task is currently interacting with */
            var prevAdapter: TasksAdapter? = null

            /** Indicates whether the end of the drag and drop has been handled */
            var isActionDragEndedHandled = false

            /**
             * Indicates the `drop zone` acting as the empty space between tasks.
             *
             * The ViewHolder item for this task must be invisible.
             */
            var dropZoneTask = Task(id = DROP_ZONE_TASK)

            /** Stores the dragged task removed from the hosting adapter.
             *
             * Use it to restore the dragged task if the drag and drop action failed.
             */
            var tempRemovedTask: Task? = null
        }

        /** Indicates whether the user has dragged the view over another adapter */
        var isNewAdapter = false


        fun startDrag(view: View, task: Task?): Boolean {
            taskInitAdapter = adapter
            prevAdapter = adapter
            isActionDragEndedHandled = false
            isNewAdapter = false

            val dragData = prepareDragData(task) ?: return false
            val taskShadow = View.DragShadowBuilder(view)

            val isSuccess = view.startDragAndDrop(dragData, taskShadow, view, 0)
            if (isSuccess) {
                view.visibility = View.INVISIBLE
            }

            return isSuccess
        }

        private fun prepareDragData(task: Task?): ClipData? {
            val json = moshi.adapter(DragAndDropTaskItem::class.java).toJson(
                DragAndDropTaskItem(
                    task = task ?: return null,
                    initPosition = draggedTaskInitPosition,
                    initAdapter = adapter.toString(),
                    initBoardList = adapter.listInfo,
                    initTasksList = adapter.tasks
                )
            )
            Log.d("ItemTaskViewHolder", "Parsed task: $json")

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

        fun handleDragEvent(receiverView: View, draggableView: View, event: DragEvent, position: Int): Boolean {

            return when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
//                        DragAndDropHelper.isActionDragEndedHandled = false
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }

                DragEvent.ACTION_DRAG_ENTERED -> {
                    Log.d(
                        "ItemTaskViewHolder",
                        "ACTION_DRAG_ENTERED: View under drag: $position\n" +
                                "currAdapter: $adapter,\n" +
                                "lastAdapter: $prevAdapter"
                    )

                    // check if the dragged item has entered an adapter other than hosting it
                    isNewAdapter = prevAdapter != adapter

                    if (isNewAdapter) {
                        // if the dragged item has entered a new adapter, remove either
                        // the previously created drop zone, or the dragged item, from the dataset
                        val isDropZoneRemoved = removeDropZone()
                        if (!isDropZoneRemoved) {
                            removeDataAt(prevAdapter!!, draggedTaskInitPosition)
                        }
                        prevAdapter = adapter
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
                                if (position < ItemTaskViewHolder.draggedTaskPrevPosition) position else position - 1
                            Log.d(
                                "ItemTaskViewHolder",
                                "ACTION_DRAG_LOCATION: oldPos: ${ItemTaskViewHolder.draggedTaskPrevPosition}, newPos: $newPos"
                            )
                            moveTask(ItemTaskViewHolder.draggedTaskPrevPosition, newPos)
                        } else if (event.y > pivot && !isMovedDown) {
                            isMovedUp = false
                            isMovedDown = true
                            val newPos =
                                if (position < ItemTaskViewHolder.draggedTaskPrevPosition) position + 1 else position
                            Log.d(
                                "ItemTaskViewHolder",
                                "ACTION_DRAG_LOCATION: oldPos: ${ItemTaskViewHolder.draggedTaskPrevPosition}, newPos: $newPos"
                            )
                            moveTask(draggedTaskPrevPosition, newPos)
                        }
                    } else if (!isInsertHandled) {
                        isInsertHandled = true
                        Log.d(
                            "ItemTaskViewHolder",
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
                    Log.d("ItemTaskViewHolder", "ACTION_DROP")
                    // why not to use the currentWorkingAdapter
//                    adapter.dragAndDropHelper.drop(
//                        clipData = event.clipData,
//                        adapter.taskDropCallbacks
//                    )

                    handleDrop(clipData = event.clipData, adapter.taskDropCallbacks)
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    if (!isActionDragEndedHandled) {
                        Log.d("ItemTaskViewHolder", "ACTION_DRAG_ENDED: handle action")
                        draggableView.visibility = View.VISIBLE
//
//                        Log.d(
//                            "ItemTaskViewHolder", "event.result: ${event.result}, " +
//                                    "tempRemovedTask: ${taskInitAdapter.dragAndDropHelper.tempRemovedTask}"
//                        )

                        // if the drag and drop failed
                        Log.d("ItemTaskViewHolder", "ACTION_ENDED: event result: ${event.result}\n" +
                                "tempRemovedTask: $tempRemovedTask")
                        if (!event.result) {
                            with(taskInitAdapter) {
                                tempRemovedTask?.let { task ->
                                    removeDropZone()
                                    if (task.id != DROP_ZONE_TASK) {
                                        Log.d(
                                            "ItemTaskViewHolder",
                                            "ACTION_DRAG_ENDED: bring back draggable item"
                                        )
                                        addData(task.position.toInt(), task)
                                        tempRemovedTask = null
                                    }
                                }
                            }

                            taskInitAdapter.notifyDataSetChanged()
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
        fun moveTask(from: Int, to: Int) {
            if (from != to && to != -1) {
                adapter.notifyItemMoved(from, to)
                draggedTaskPrevPosition = to
                Log.d("ItemTaskViewHolder", "move: Moved item from $from to $to")
            }
        }

        /**
         * Removes the drop zone from the [RecyclerView] and [TasksAdapter] datasets
         */
        fun removeDropZone(): Boolean {
            val isDropZoneTaskPresent = adapter.tasks.any { it.id == DROP_ZONE_TASK }
            return if (isDropZoneTaskPresent) {
                // when we move items only the underlying dataset gets updated,
                // so in the tasks list we remove dragShadowTask at its insert position...
                adapter.tasks.removeAt(dropZoneTask.position.toInt())
                dropZoneTask = dropZoneTask.copy(position = -1)
                // ...but in the underlying dataset we remove item at the last moved position
                adapter.notifyItemRemoved(ItemTaskViewHolder.draggedTaskPrevPosition)
                Log.d(
                    "ItemTaskViewHolder",
                    "removeDropZone: removed drop zone at position ${ItemTaskViewHolder.draggedTaskPrevPosition}"
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
        fun removeDataAt(adapter: TasksAdapter, position: Int) {
            if (position != -1) {
                tempRemovedTask = adapter.tasks[position]
                Log.d(
                    "ItemTaskViewHolder",
                    "removeDataAt: tempRemovedTask: $tempRemovedTask"
                )
                // when we move items only the underlying dataset gets updated,
                // so in the tasks list we remove item at its insert position...
                adapter.tasks.removeAt(position)
                // ...but in the underlying dataset we remove item at the last moved position
                adapter.notifyItemRemoved(draggedTaskPrevPosition)
                draggedTaskPrevPosition = -1 // TODO: Check if this reset is vital
                Log.d("ItemTaskViewHolder", "removeData: removed item at $position")
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
        fun insertDropZone(adapter: TasksAdapter, position: Int) {
            if (!adapter.tasks.any { it.id == DROP_ZONE_TASK }) {
                dropZoneTask = dropZoneTask.copy(position = position.toLong())
                adapter.tasks.add(position, dropZoneTask)
                adapter.notifyItemInserted(position)
                draggedTaskPrevPosition = position
                Log.d("ItemTaskViewHolder", "insertDropZone: inserted drop_zone")
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
        fun handleDrop(clipData: ClipData, taskDropCallbacks: TaskDropCallbacks): Boolean {
            val data = clipData.getItemAt(0).text.toString()
            val moshi = Moshi.Builder().build()
            val jsonAdapter = moshi.adapter(DragAndDropTaskItem::class.java)
            val dragItem = jsonAdapter.fromJson(data)

            return if (dragItem == null) {
                Log.d(
                    "ItemTaskViewHolder",
                    "drop: dragItem is null"
                )
                false
            } else {
                val containedDropZone = removeDropZone()
                Log.d(
                    "ItemTaskViewHolder",
                    "drop: isNewAdapter: ${isNewAdapter}, " +
                            "containedDropZone: $containedDropZone"
                )

                if (isNewAdapter || containedDropZone) {
                    Log.d(
                        "ItemTaskViewHolder",
                        "drop: insert task ${dragItem.task}"
                    )

                    taskDropCallbacks.dropToInsert(
                        adapter,
                        dragItem,
                        ItemTaskViewHolder.draggedTaskPrevPosition
                    )
                } else {
                    Log.d(
                        "ItemTaskViewHolder",
                        "drop: move tasks from ${dragItem.initPosition} to ${ItemTaskViewHolder.draggedTaskPrevPosition}"
                    )
                    taskDropCallbacks.dropToMove(
                        adapter,
                        dragItem.initPosition,
                        ItemTaskViewHolder.draggedTaskPrevPosition
                    )
                }
                true
            }
        }
    }
}