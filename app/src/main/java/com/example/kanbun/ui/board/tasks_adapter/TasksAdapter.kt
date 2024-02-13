package com.example.kanbun.ui.board.tasks_adapter

import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Color
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.databinding.ItemTaskBinding
import com.example.kanbun.databinding.ItemTaskTagBinding
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.board.TaskDropCallbacks
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.ui.model.DragAndDropTaskItem
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

/**
 * A [RecyclerView.Adapter] responsible for managing and displaying a list of [Task]s
 *
 * @param rvTasks the reference to a [RecyclerView] using this adapter
 * @property taskDropCallbacks the set of callbacks used when the dragged item gets dropped
 * @property onTaskClickListener the callback called when the user click on a task item
 */
class TasksAdapter(
    rvTasks: RecyclerView,
    private val boardTags: List<Tag>,
    private val taskDropCallbacks: TaskDropCallbacks,
    private val onTaskClicked: (Task) -> Unit,
) : RecyclerView.Adapter<TasksAdapter.ItemTaskViewHolder>() {
    var tasks: MutableList<Task> = mutableListOf()
        private set

    lateinit var listInfo: BoardListInfo

    init {
        rvTasks.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }

                DragEvent.ACTION_DROP -> {
                    Log.d("ItemTaskViewHolder", "RecView#ACTION_DROP at $this")
                    dragAndDropHelper.drop(
                        event.clipData,
                        taskDropCallbacks
                    )
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    Log.d("ItemTaskViewHolder", "RecView#ACTION_DRAG_ENDED")
                    dragAndDropHelper.removeDropZone()
                    true
                }

                else -> false
            }
        }
    }

    private val dragAndDropHelper = DragAndDropHelper(this)

    fun setData(data: List<Task>) {
        tasks = data.toMutableList()
        Log.d("TasksAdapter", "adapter: ${this}\tsetData: $data")
        notifyDataSetChanged()
    }

    fun addData(position: Int, task: Task) {
        tasks.add(position, task)
        notifyDataSetChanged()
        Log.d("ItemTaskViewHolder", "addData: Added task $task at position $position")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTaskViewHolder {
        Log.d("TasksAdapter", "onCreateViewHolder is called")
        return ItemTaskViewHolder(
            binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            boardTags = boardTags,
            tasksAdapter = this@TasksAdapter
        ) { position ->
            onTaskClicked(tasks[position])
        }
    }

    override fun onBindViewHolder(holder: ItemTaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

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

        init {
            binding.materialCard.setOnClickListener {
                clickAtPosition(adapterPosition)
            }

            // set up on long click listener that initiates the dragging motion
            binding.materialCard.setOnLongClickListener { view ->
                Log.d(
                    "ItemTaskViewHolder",
                    "item clicked pos: $adapterPosition, item clicked adapter: $tasksAdapter"
                )

                DragAndDropHelper.draggedTaskAdapter = tasksAdapter
                DragAndDropHelper.currentInteractingAdapter = tasksAdapter
                DragAndDropHelper.isActionDragEndedHandled = false
                draggedTaskInitPosition = adapterPosition
                draggedTaskPrevPosition = adapterPosition
                tasksAdapter.dragAndDropHelper.isNewAdapter = false

                // TODO: Provide singleton implementation, or inject with Hilt
                val moshi = Moshi.Builder().build()
                val jsonAdapter: JsonAdapter<DragAndDropTaskItem> =
                    moshi.adapter(DragAndDropTaskItem::class.java)
                val json =
                    jsonAdapter.toJson(
                        DragAndDropTaskItem(
                            task = task ?: throw NullPointerException("Task can't be null"),
                            initPosition = draggedTaskInitPosition,
                            initAdapter = tasksAdapter.toString(),
                            initBoardList = tasksAdapter.listInfo,
                            initTasksList = tasksAdapter.tasks
                        )
                    )
                Log.d("ItemTaskViewHolder", "Parsed task: $json")

                val item = ClipData.Item(json)
                val dataToDrag = ClipData(
                    "task_json",
                    arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                    item
                )
                val taskShadow = View.DragShadowBuilder(view)

                with(view) {
                    val isStartSuccess = startDragAndDrop(dataToDrag, taskShadow, view, 0)
                    if (isStartSuccess) {
                        visibility = View.INVISIBLE
                    }
                }

                true
            }

            // flags indicating the movement state of the draggable item over a task's drag listener
            var isMovedUp = false
            var isMovedDown = false
            var isInsertHandled = false

            binding.materialCard.setOnDragListener { _, event ->
                val draggableView = event?.localState as View

                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
//                        DragAndDropHelper.isActionDragEndedHandled = false
                        event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    }

                    DragEvent.ACTION_DRAG_ENTERED -> {
                        Log.d(
                            "ItemTaskViewHolder",
                            "ACTION_DRAG_ENTERED: View under drag: $adapterPosition\n" +
                                    "currAdapter: $tasksAdapter,\n" +
                                    "lastAdapter: ${DragAndDropHelper.currentInteractingAdapter}"
                        )

                        // check if the dragged item has entered an adapter other than hosting it
                        tasksAdapter.dragAndDropHelper.isNewAdapter =
                            DragAndDropHelper.currentInteractingAdapter != tasksAdapter

                        if (tasksAdapter.dragAndDropHelper.isNewAdapter) {
                            // if the dragged item has entered a new adapter, remove from the dataset
                            // either the previously created drop zone, or the dragged item
                            val isDropZoneRemoved =
                                DragAndDropHelper.currentInteractingAdapter?.dragAndDropHelper?.removeDropZone()

                            if (isDropZoneRemoved != true) {
                                DragAndDropHelper.currentInteractingAdapter?.dragAndDropHelper?.removeDataAt(
                                    draggedTaskInitPosition
                                )
                            }
                            DragAndDropHelper.currentInteractingAdapter = tasksAdapter
                        }
                        true
                    }

                    DragEvent.ACTION_DRAG_LOCATION -> {
                        val pivot = draggableView.height / 2

                        if (!tasksAdapter.dragAndDropHelper.isNewAdapter) {
                            if (event.y < pivot && !isMovedUp) {
                                isMovedDown = false
                                isMovedUp = true
                                val newPos =
                                    if (adapterPosition < draggedTaskPrevPosition) adapterPosition else adapterPosition - 1
                                Log.d(
                                    "ItemTaskViewHolder",
                                    "ACTION_DRAG_LOCATION: oldPos: $draggedTaskPrevPosition, newPos: $newPos"
                                )
                                DragAndDropHelper.currentInteractingAdapter?.dragAndDropHelper?.move(
                                    draggedTaskPrevPosition,
                                    newPos
                                )
                            } else if (event.y > pivot && !isMovedDown) {
                                isMovedUp = false
                                isMovedDown = true
                                val newPos =
                                    if (adapterPosition < draggedTaskPrevPosition) adapterPosition + 1 else adapterPosition
                                Log.d(
                                    "ItemTaskViewHolder",
                                    "ACTION_DRAG_LOCATION: oldPos: $draggedTaskPrevPosition, newPos: $newPos"
                                )
                                DragAndDropHelper.currentInteractingAdapter?.dragAndDropHelper?.move(
                                    draggedTaskPrevPosition,
                                    newPos
                                )
                            }
                        } else if (!isInsertHandled) {
                            isInsertHandled = true
                            Log.d(
                                "ItemTaskViewHolder",
                                "ACTION_DRAG_LOCATION: insertPos: $adapterPosition"
                            )
                            DragAndDropHelper.currentInteractingAdapter?.dragAndDropHelper?.insertDropZone(
                                tasksAdapter,
                                adapterPosition
                            )
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
                        tasksAdapter.dragAndDropHelper.drop(
                            clipData = event.clipData,
                            tasksAdapter.taskDropCallbacks
                        )
                    }

                    DragEvent.ACTION_DRAG_ENDED -> {
                        if (!DragAndDropHelper.isActionDragEndedHandled) {
                            Log.d("ItemTaskViewHolder", "ACTION_DRAG_ENDED: handle action")
                            draggableView.visibility = View.VISIBLE

                            Log.d(
                                "ItemTaskViewHolder", "event.result: ${event.result}, " +
                                        "tempRemovedTask: ${DragAndDropHelper.draggedTaskAdapter.dragAndDropHelper.tempRemovedTask}"
                            )

                            // if the drag and drop failed
                            if (!event.result) {
                                with(DragAndDropHelper.draggedTaskAdapter) {
                                    dragAndDropHelper.tempRemovedTask?.let { task ->
                                        dragAndDropHelper.removeDropZone()
                                        if (task.id != DragAndDropHelper.DROP_ZONE_TASK) {
                                            Log.d(
                                                "ItemTaskViewHolder",
                                                "ACTION_DRAG_ENDED: bring back draggable item"
                                            )
                                            addData(task.position.toInt(), task)
                                            dragAndDropHelper.tempRemovedTask = null
                                        }
                                    }
                                }

                                DragAndDropHelper.draggedTaskAdapter.notifyDataSetChanged()
                            }

                            DragAndDropHelper.isActionDragEndedHandled = true
                        }

                        true
                    }

                    else -> false
                }
            }
        }

        fun bind(task: Task) {
            Log.d("TasksAdapter", "bind:\ttask: $task")
            this.task = task

            binding.apply {
                materialCard.visibility = if (task.id != DragAndDropHelper.DROP_ZONE_TASK) {
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
    }

    /**
     * A helper class for managing drag-and-drop interactions and states within the [TasksAdapter]
     *
     * @property adapter the [TasksAdapter] instance associated with this drag-and-drop helper
     */
    private class DragAndDropHelper(private val adapter: TasksAdapter) {
        companion object {
            const val DROP_ZONE_TASK = "drop_zone"

            /** The dragged task's hosting [TasksAdapter] */
            lateinit var draggedTaskAdapter: TasksAdapter

            /** The [TasksAdapter] the dragged task is currently interacting with */
            var currentInteractingAdapter: TasksAdapter? = null

            /** Indicates whether the end of the drag and drop has been handled */
            var isActionDragEndedHandled = false

            /**
             * Indicates the `drop zone` acting as the empty space between tasks.
             *
             * The ViewHolder item for this task must be invisible.
             */
            var dropZoneTask = Task(id = DROP_ZONE_TASK)
        }

        /** Indicates whether the user has dragged the view over another adapter */
        var isNewAdapter = false

        /** Stores the dragged task removed from the hosting adapter.
         *
         * Use it to restore the dragged task if the drag and drop action failed.
         */
        var tempRemovedTask: Task? = null

        /**
         * Moves the task item in the [RecyclerView] list.
         *
         * @param from the position to move from
         * @param to the position to move to
         */
        fun move(from: Int, to: Int) {
            if (from != to && to != -1) {
                adapter.notifyItemMoved(from, to)
                ItemTaskViewHolder.draggedTaskPrevPosition = to
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
                    "removeDropZone: removed drop zone at position ${TasksAdapter.ItemTaskViewHolder.draggedTaskPrevPosition}"
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
        fun removeDataAt(position: Int) {
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
                adapter.notifyItemRemoved(ItemTaskViewHolder.draggedTaskPrevPosition)
                ItemTaskViewHolder.draggedTaskPrevPosition =
                    -1 // TODO: Check if this reset is vital
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
                ItemTaskViewHolder.draggedTaskPrevPosition = position
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
        fun drop(clipData: ClipData, taskDropCallbacks: TaskDropCallbacks): Boolean {
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