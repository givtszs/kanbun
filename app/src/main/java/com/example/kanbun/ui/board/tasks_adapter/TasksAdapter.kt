package com.example.kanbun.ui.board.tasks_adapter

import android.content.ClipData
import android.content.ClipDescription
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemTaskBinding
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.board.DropCallback
import com.example.kanbun.ui.model.BoardListInfo
import com.example.kanbun.ui.model.DragAndDropTaskItem
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

interface DragCallbacks {
    fun move(from: Int, to: Int)
    fun removeDropZone(): Boolean

    fun removeDataAt(position: Int)
    fun insertDropZone(adapter: TasksAdapter, position: Int)

}

private const val DROP_ZONE_TASK = "drop_zone"

private lateinit var draggedTaskAdapter: TasksAdapter
private var currentInteractingAdapter: TasksAdapter? = null
private var isActionDragEndedHandled = false
private var dropZoneTask = Task(id = DROP_ZONE_TASK)

class TasksAdapter(
    private val dropCallback: DropCallback,
    private val onTaskClickListener: (Task) -> Unit
) : RecyclerView.Adapter<TasksAdapter.ItemTaskViewHolder>() {
    var tasks: MutableList<Task> = mutableListOf()

    lateinit var listInfo: BoardListInfo
    var isNewAdapter = false
    var tempRemovedTask: Pair<Int, Task>? = null
    
    val dragCallbacks = object : DragCallbacks {
        override fun move(from: Int, to: Int) {
            if (from != to && to != -1) {
                notifyItemMoved(from, to)
                ItemTaskViewHolder.oldPosition = to
                Log.d("ItemTaskViewHolder", "move: Moved item from $from to $to")
            }
        }

        override fun removeDropZone(): Boolean {
            val isDropZoneTaskPresent = tasks.any { it.id == DROP_ZONE_TASK }
            return if (isDropZoneTaskPresent) {
                // when we move items only the underlying dataset gets updated,
                // so in the tasks list we remove dragShadowTask at its insert position...
                tasks.removeAt(dropZoneTask.position.toInt())
                dropZoneTask = dropZoneTask.copy(position = -1)
                // ...but in the underlying dataset we remove item at the last moved position
                notifyItemRemoved(ItemTaskViewHolder.oldPosition)
                Log.d("ItemTaskViewHolder", "removeDropZone: removed drop zone at position ${ItemTaskViewHolder.oldPosition}")
                true
            } else {
                false
            }
        }

        override fun removeDataAt(position: Int) {
            if (position != -1) {
                tempRemovedTask = Pair(position, tasks[position])
                Log.d("ItemTaskViewHolder", "removeDataAt: tempRemovedTask: ${tempRemovedTask?.second}")
                // when we move items only the underlying dataset gets updated,
                // so in the tasks list we remove item at its insert position...
                tasks.removeAt(position)
                // ...but in the underlying dataset we remove item at the last moved position
                notifyItemRemoved(ItemTaskViewHolder.oldPosition)
                ItemTaskViewHolder.oldPosition = -1 // TODO: Check if this reset is vital
                Log.d("ItemTaskViewHolder", "removeData: removed item at $position")
            }
        }

        override fun insertDropZone(adapter: TasksAdapter, position: Int) {
            if (!tasks.any { it.id == DROP_ZONE_TASK }) {
                dropZoneTask = dropZoneTask.copy(position = position.toLong())
                tasks.add(position, dropZoneTask)
                notifyItemInserted(position)
                ItemTaskViewHolder.oldPosition = position
                Log.d("ItemTaskViewHolder", "insertDropZone: inserted drop_zone")
            }
        }
    }

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

                isActionDragEndedHandled = false
                draggedTaskAdapter = tasksAdapter
                currentInteractingAdapter = tasksAdapter
                initPosition = adapterPosition
                ItemTaskViewHolder.oldPosition = adapterPosition
                tasksAdapter.isNewAdapter = false

                val moshi = Moshi.Builder().build()
                val jsonAdapter: JsonAdapter<DragAndDropTaskItem> =
                    moshi.adapter(DragAndDropTaskItem::class.java)
                val json =
                    jsonAdapter.toJson(
                        DragAndDropTaskItem(
                            task = task ?: throw NullPointerException("Task can't be null"),
                            initPosition = initPosition,
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

            // flags indicating the movement state of the draggable item over drag listener
            var isMovedUp = false
            var isMovedDown = false
            var isInsertHandled = false

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
                                    "lastAdapter: $currentInteractingAdapter"
                        )

                        tasksAdapter.isNewAdapter = currentInteractingAdapter != tasksAdapter
                        if (tasksAdapter.isNewAdapter) {
                            val isShadowRemoved = currentInteractingAdapter?.dragCallbacks?.removeDropZone()
                            if (isShadowRemoved != true) {
                                // remove the draggable item from the hosting list
                                // active if the user moves the item to another list
                                currentInteractingAdapter?.dragCallbacks?.removeDataAt(initPosition)
                            }
                            currentInteractingAdapter = tasksAdapter
                        }
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
                                    "ACTION_DRAG_LOCATION: oldPos: ${ItemTaskViewHolder.oldPosition}, newPos: $newPos")
                                currentInteractingAdapter?.dragCallbacks?.move(ItemTaskViewHolder.oldPosition, newPos)
                            } else if (event.y > pivot && !isMovedDown) {
                                isMovedUp = false
                                isMovedDown = true
                                newPos =
                                    if (adapterPosition < ItemTaskViewHolder.oldPosition) adapterPosition + 1 else adapterPosition
                                Log.d(
                                    "ItemTaskViewHolder",
                                    "ACTION_DRAG_LOCATION: oldPos: ${ItemTaskViewHolder.oldPosition}, newPos: $newPos")
                                currentInteractingAdapter?.dragCallbacks?.move(ItemTaskViewHolder.oldPosition, newPos)
                            }
                        } else if (!isInsertHandled) {
                            isInsertHandled = true
                            Log.d(
                                "ItemTaskViewHolder",
                                "ACTION_DRAG_LOCATION: insertPos: $adapterPosition"
                            )
                            currentInteractingAdapter?.dragCallbacks?.insertDropZone(tasksAdapter, adapterPosition)
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
                        tasksAdapter.dropCallback.drop(
                            clipData = event.clipData,
                            adapter = tasksAdapter,
                            position = ItemTaskViewHolder.oldPosition
                        )
                    }

                    DragEvent.ACTION_DRAG_ENDED -> {
                        if (!isActionDragEndedHandled) {
                            Log.d("ItemTaskViewHolder", "ACTION_DRAG_ENDED: handle action")
                            draggableView.visibility = View.VISIBLE

                            Log.d("ItemTaskViewHolder", "event.result: ${event.result}, " +
                                    "tempRemovedTask: ${draggedTaskAdapter.tempRemovedTask}")

                            if (!event.result) {
                                draggedTaskAdapter.dragCallbacks.removeDropZone()
                                draggedTaskAdapter.tempRemovedTask?.let {
                                    if (it.second.id != DROP_ZONE_TASK) {
                                        Log.d("ItemTaskViewHolder", "ACTION_DRAG_ENDED: bring back draggable item")
                                        draggedTaskAdapter.addData(it.first, it.second)
                                        draggedTaskAdapter.tempRemovedTask = null
                                    }
                                }

                                draggedTaskAdapter.notifyDataSetChanged()
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
                materialCard.visibility = if (task.id != DROP_ZONE_TASK) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
            }

            this.task = task
        }
    }
}