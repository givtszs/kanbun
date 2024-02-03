package com.example.kanbun.ui.board.tasks_adapter

import android.content.ClipData
import android.content.ClipDescription
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemTaskBinding
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.board.DropCallbacks
import com.example.kanbun.ui.model.BoardListInfo
import com.example.kanbun.ui.model.DragAndDropTaskItem
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlin.properties.Delegates

interface DragCallbacks {
    fun move(from: Int, to: Int)
    fun removeDragShadow(): Boolean

    fun removeDataAt(position: Int)
    fun insertDragShadow(adapter: TasksAdapter, position: Int)

}

private lateinit var draggedTaskAdapter: TasksAdapter
private var currentVisibleAdapter: TasksAdapter? = null
private var isActionDragEndedHandled = false
private var dragShadowTask = Task(id = "drag_shadow")

class TasksAdapter(
    private val dropCallbacks: DropCallbacks,
    private val onTaskClickListener: (Task) -> Unit
) : RecyclerView.Adapter<TasksAdapter.ItemTaskViewHolder>(), DragCallbacks {
    var tasks: MutableList<Task> = mutableListOf()

    companion object {
//        var currentVisibleAdapter: TasksAdapter? = null
//        var isNewAdapter = false
    }
//    var isNewAdapter = false
//    private var taskOldPosition = -1
//    private lateinit var draggedViewAdapter: TasksAdapter

    lateinit var listInfo: BoardListInfo

    //    lateinit var draggedTask: DragAndDropTaskItem
//    var toPosition = -1

    fun setData(data: List<Task>) {
        tasks = data.toMutableList()
        Log.d("TasksAdapter", "adapter: ${this}\tsetData: $data")
        notifyDataSetChanged()
    }

    fun addData(position: Int, task: Task) {
        tasks.add(position, task)
        notifyItemInserted(position)

        val updTasks = buildString {
            tasks.forEach {
                append("${it.id}, ")
            }
        }
        Log.d(
            "ItemTaskViewHolder", "addData: Added task $task at position $position\n" +
                    "tasks: $updTasks"
        )
    }

    override fun move(from: Int, to: Int) {
        if (from != to && to != -1) {
            notifyItemMoved(from, to)
            ItemTaskViewHolder.oldPosition = to
//            toPosition = to
            val updTasks = buildString {
                tasks.forEach {
                    append("${it.id}, ")
                }
            }


            // TODO: since we only move tasks in the recyclerview and not the actual List itself,
            //  inspect and update insertDragShadow and other methods


            Log.d(
                "ItemTaskViewHolder", "move: Moved item from $from to $to\n" +
                        "tasks: $updTasks"
            )
        }
    }

    override fun removeDragShadow(): Boolean {
        val isDragShadowTaskPresent = tasks.any { it.id == "drag_shadow" }
        return if (isDragShadowTaskPresent) {
            // when we move items only the underlying dataset gets updated,
            // so in the tasks list we remove dragShadowTask at its insert position...
            tasks.removeAt(dragShadowTask.position.toInt())
            dragShadowTask = dragShadowTask.copy(position = -1)
            // ...but in the underlying dataset we remove item at the last moved position
            notifyItemRemoved(ItemTaskViewHolder.oldPosition)
            containsDragShadow = false

            val updTasks = buildString {
                tasks.forEach {
                    append("${it.id}, ")
                }
            }

            Log.d("ItemTaskViewHolder", "removeDragShadow: removed visual drag_shadow at position ${ItemTaskViewHolder.oldPosition}\n" +
                    "tasks: $updTasks")
            true
        } else {
            false
        }

//        val shadowTask = tasks.find { it.id == "drag_shadow" }
//        if (shadowTask != null) {
//            removeDataAt(ItemTaskViewHolder.oldPosition)
//
//            // insert a task with firestore repository methods
//
////            draggedViewAdapter.tasks.add(draggedTask!!.first, draggedTask!!.second)
////            draggedViewAdapter.notifyItemInserted(draggedTask!!.first)
//
//        }
    }

    var isNewAdapter = false
    var tempRemovedTask: Pair<Int, Task>? = null
    var containsDragShadow = false

    override fun removeDataAt(position: Int) {
        if (position != -1) {
            tempRemovedTask = Pair(position, tasks[position])
//            tempRemovedTask = Pair(position, tasks[ItemTaskViewHolder.initTaskPosition])
            Log.d("ItemTaskViewHolder", "removeDataAt: tempRemovedTask: ${tempRemovedTask?.second}")
            // when we move items only the underlying dataset gets updated,
            // so in the tasks list we remove item at its insert position...
            tasks.removeAt(position)
            // ...but in the underlying dataset we remove item at the last moved position
            notifyItemRemoved(ItemTaskViewHolder.oldPosition)
            ItemTaskViewHolder.oldPosition = -1 // TODO: Check if this reset is vital

            val updTasks = buildString {
                tasks.forEach {
                    append("${it.id}, ")
                }
            }
            Log.d("ItemTaskViewHolder", "removeData: removed item at $position\ntasks: $updTasks")
        }
    }

    // TODO: if method works, remove adapter parameter

    override fun insertDragShadow(adapter: TasksAdapter, position: Int) {
        if (!tasks.contains(Task(id = "drag_shadow"))) {
            dragShadowTask = dragShadowTask.copy(position = position.toLong())
            tasks.add(position, dragShadowTask)
            notifyItemInserted(position)
            containsDragShadow = true

            val updTasks = buildString {
                tasks.forEach {
                    append("${it.id}, ")
                }
            }

            ItemTaskViewHolder.oldPosition = position

            Log.d("ItemTaskViewHolder", "insertDragShadow: inserted drag_shadow\ntasks: $updTasks")
        }

//        toPosition = position
//        currentVisibleAdapter = adapter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTaskViewHolder {
        Log.d("TasksAdapter", "onCreateViewHolder is called")
        return ItemTaskViewHolder(
            binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            tasksAdapter = this@TasksAdapter
        ) { position ->
            onTaskClickListener(tasks[position])
        }
    }

    override fun onBindViewHolder(holder: ItemTaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    class ItemTaskViewHolder(
        private val binding: ItemTaskBinding,
        private val tasksAdapter: TasksAdapter,
        private val clickAtPosition: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            var initPosition = -1
            var oldPosition = -1
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

//                TasksAdapter.taskOldPosition = adapterPosition
                // set up top-level fields
//                taskOldPosition = adapterPosition
                isActionDragEndedHandled = false
                draggedTaskAdapter = tasksAdapter
                currentVisibleAdapter = tasksAdapter

                initPosition = adapterPosition
                ItemTaskViewHolder.oldPosition = adapterPosition
                tasksAdapter.isNewAdapter = false
//                tasksAdapter.tempRemovedTask = Pair(adapterPosition, task!!)

//                draggedViewAdapter = tasksAdapter
//                tasksAdapter.draggedTask =

                val moshi = Moshi.Builder().build()
                val jsonAdapter: JsonAdapter<DragAndDropTaskItem> =
                    moshi.adapter(DragAndDropTaskItem::class.java)
                val json =
                    jsonAdapter.toJson(
                        DragAndDropTaskItem(
                            task = task ?: throw NullPointerException("Task can't be null"),
                            initPosition = adapterPosition,
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


            var isMovedUp = false
            var isMovedDown = false
            var isInsertHandled = false
//            var isNewAdapter = false

            binding.materialCard.setOnDragListener { _, event ->
                val draggableView = event?.localState as View

                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        val clipDescription = event.clipDescription
                        clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    }

                    DragEvent.ACTION_DRAG_ENTERED -> {
                        Log.d(
                            "ItemTaskViewHolder",
                            "ACTION_DRAG_ENTERED: View under drag: $adapterPosition\n" +
                                    "currAdapter: $tasksAdapter,\n" +
                                    "lastAdapter: $currentVisibleAdapter"
                        )

                        tasksAdapter.isNewAdapter = currentVisibleAdapter != tasksAdapter
                        if (tasksAdapter.isNewAdapter) {
                            // TODO: Update to delete either drag_shadow or initPosition
//                            currentVisibleAdapter?.removeDataAt(ItemTaskViewHolder.oldPosition)
                            val isShadowRemoved = currentVisibleAdapter?.removeDragShadow() // TODO: EXPERIMENTAL
                            if (isShadowRemoved != true) {
                                currentVisibleAdapter?.removeDataAt(initPosition)
                            }
                            currentVisibleAdapter = tasksAdapter
                        }
//                        // check if the current adapter user interacting with is new
//                        isNewAdapter = currentVisibleAdapter != tasksAdapter
//                        if (isNewAdapter) {
//                            currentVisibleAdapter?.removeDataAt(taskOldPosition)
//                        }
                        true
                    }

                    DragEvent.ACTION_DRAG_LOCATION -> {
                        val pivot = draggableView.height / 2

                        val newPos: Int
                        if (!tasksAdapter.isNewAdapter) {
                            if (event.y < pivot && !isMovedUp) {
                                isMovedDown = false
                                isMovedUp = true
                                newPos =
                                    if (adapterPosition < ItemTaskViewHolder.oldPosition) ItemTaskViewHolder.oldPosition - 1 else adapterPosition - 1
                                Log.d(
                                    "ItemTaskViewHolder",
                                    "ACTION_DRAG_LOCATION: oldPos: ${ItemTaskViewHolder.oldPosition}," +
                                            " newPos: $newPos"
                                )
                                currentVisibleAdapter?.move(ItemTaskViewHolder.oldPosition, newPos)
                            } else if (event.y > pivot && !isMovedDown) {
                                isMovedUp = false
                                isMovedDown = true
                                newPos =
                                    if (adapterPosition < ItemTaskViewHolder.oldPosition) adapterPosition + 1 else adapterPosition
                                Log.d(
                                    "ItemTaskViewHolder",
                                    "ACTION_DRAG_LOCATION: oldPos: ${ItemTaskViewHolder.oldPosition}," +
                                            " newPos: $newPos"
                                )
                                currentVisibleAdapter?.move(ItemTaskViewHolder.oldPosition, newPos)
                            }
                        } else if (!isInsertHandled) {
                            isInsertHandled = true
                            Log.d(
                                "ItemTaskViewHolder",
                                "ACTION_DRAG_LOCATION: insertPos: $adapterPosition"
                            )
                            currentVisibleAdapter?.insertDragShadow(tasksAdapter, adapterPosition)
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
                        val data = event.clipData.getItemAt(0).text.toString()
                        Log.d("ItemTaskViewHolder", "ACTION_DROP: clip data: $data")

                        val moshi = Moshi.Builder().build()
                        val jsonAdapter = moshi.adapter(DragAndDropTaskItem::class.java)
                        val dragItem = jsonAdapter.fromJson(data)

                        if (dragItem == null) {
                            Log.d(
                                "ItemTaskViewHolder",
                                "ACTION_DROP: dragItem is null"
                            )
                            false
                        } else {
//                            Log.d(
//                                "ItemTaskViewHolder",
//                                "ACTION_DROP: are adapters the same: ${dragItem.initAdapter == tasksAdapter.toString()}"
//                            )
                            val containedDragShadow = tasksAdapter.removeDragShadow()
                            Log.d("ItemTaskViewHolder", "ACTION_DROP: isNewAdapter: ${tasksAdapter.isNewAdapter}, " +
                                    "containsDragShadow: ${tasksAdapter.containsDragShadow}")

                            if (tasksAdapter.isNewAdapter || containedDragShadow) {
                                Log.d(
                                    "ItemTaskViewHolder",
                                    "ACTION_DROP: insert task ${dragItem.task}"
                                )

                                tasksAdapter.dropCallbacks.dropToInsert(
                                    adapterToInsert = tasksAdapter,
                                    dragItem,
                                    ItemTaskViewHolder.oldPosition
                                )
                            } else {
                                Log.d(
                                    "ItemTaskViewHolder",
                                    "ACTION_DROP: move tasks from ${dragItem.initPosition} to ${ItemTaskViewHolder.oldPosition}"
                                )
                                tasksAdapter.dropCallbacks.dropToMove(
                                    tasksAdapter,
                                    dragItem.initPosition,
                                    ItemTaskViewHolder.oldPosition
                                )
                            }
                            true
                        }
                    }

                    DragEvent.ACTION_DRAG_ENDED -> {
                        draggableView.visibility = View.VISIBLE
                        if (!isActionDragEndedHandled) {
                            Log.d("ItemTaskViewHolder", "ACTION_DRAG_ENDED: handle action")
//                            tasksAdapter.toPosition = -1
//                            tasksAdapter.removeDragShadow()
                            ItemTaskViewHolder.oldPosition = -1
//                            draggedTaskAdapter.tempRemovedTask?.let {
//                                draggedTaskAdapter.addData(it.first, it.second)
//                                draggedTaskAdapter.tempRemovedTask = null
//                            }
//                            if (!event.result) {
//                            }

                            draggedTaskAdapter.tempRemovedTask?.let {
                                if (it.second.id != "drag_shadow") {
                                    Log.d("ItemTaskViewHolder", "ACTION_DRAG_ENDED: bring back draggable item")
                                    draggedTaskAdapter.addData(it.first, it.second)
                                    draggedTaskAdapter.tempRemovedTask = null
                                }
                            }
                            isActionDragEndedHandled = true
                        }

                        true
                    }

                    else -> false
                }
            }
        }

        fun bind(task: Task) {
            Log.d("TasksAdapter", "bind:\ttask: $task")
            binding.apply {
                tvName.text = task.name
            }

            if (task.id == "drag_shadow") {
                binding.materialCard.visibility = View.INVISIBLE
            } else {
                binding.materialCard.visibility = View.VISIBLE
            }

            this.task = task
        }
    }
}