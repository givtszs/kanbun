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

interface DragCallbacks {
    fun move(from: Int, to: Int)
    fun removeDragShadow()

    fun removeDataAt(position: Int)
    fun insertDragShadow(adapter: TasksAdapter, position: Int)

}

class TasksAdapter(
    private val dropCallbacks: DropCallbacks,
    private val onTaskClickListener: (Task) -> Unit
) : RecyclerView.Adapter<TasksAdapter.ItemTaskViewHolder>(), DragCallbacks {
    var tasks: MutableList<Task> = mutableListOf()

    companion object {
        var currentVisibleAdapter: TasksAdapter? = null
//        var isNewAdapter = false
    }
//    var isNewAdapter = false
//    private var taskOldPosition = -1
//    private lateinit var draggedViewAdapter: TasksAdapter

    lateinit var listInfo: BoardListInfo
//    lateinit var draggedTask: DragAndDropTaskItem
    var toPosition = -1

    fun setData(data: List<Task>) {
        tasks = data.toMutableList()
        Log.d("TasksAdapter", "adapter: ${this}\tsetData: $data")
        notifyDataSetChanged()
    }

    override fun move(from: Int, to: Int) {
        if (from != to && to != -1) {
            notifyItemMoved(from, to)
            Log.d("ItemTaskViewHolder", "moved item from $from to $to")
            ItemTaskViewHolder.oldPosition = to
            toPosition = to
        }
    }

    override fun removeDragShadow() {
        val shadowTask = tasks.find { it.id == "drag_shadow" }

        if (shadowTask != null) {
            removeDataAt(ItemTaskViewHolder.oldPosition)

            // insert a task with firestore repository methods

//            draggedViewAdapter.tasks.add(draggedTask!!.first, draggedTask!!.second)
//            draggedViewAdapter.notifyItemInserted(draggedTask!!.first)

            Log.d("ItemTaskViewHolder", "Drag shadow task removed. Dragged task reset")
        }
    }

    override fun removeDataAt(position: Int) {
        if (position != -1) {
//            tasks.removeAt(position)
            notifyItemRemoved(position)
            ItemTaskViewHolder.oldPosition = -1

            Log.d("ItemTaskViewHolder", "removed item at $position")
        }
    }

    // TODO: if method works, remove adapter parameter
    override fun insertDragShadow(adapter: TasksAdapter, position: Int) {
        tasks.add(position, Task(id = "drag_shadow"))

        val updTasks = buildString {
            tasks.forEach {
                append("${it.id}, ")
            }
        }
        Log.d("ItemTaskViewHolder", "Updated tasks: $updTasks")

        notifyItemInserted(position)

        Log.d("ItemTaskViewHolder", "inserted item at $position")
        ItemTaskViewHolder.oldPosition = position
        toPosition = position
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
                currentVisibleAdapter = tasksAdapter
                ItemTaskViewHolder.oldPosition = adapterPosition
                ItemTaskViewHolder.oldPosition = adapterPosition

//                draggedViewAdapter = tasksAdapter
//                tasksAdapter.draggedTask =

                val moshi = Moshi.Builder().build()
                val jsonAdapter: JsonAdapter<DragAndDropTaskItem> =
                    moshi.adapter(DragAndDropTaskItem::class.java)
                val json =
                    jsonAdapter.toJson(DragAndDropTaskItem(task ?: throw NullPointerException("Task can't be null"), adapterPosition, tasksAdapter.toString(), tasksAdapter.tasks))
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
            var isNewAdapter = false

            binding.materialCard.setOnDragListener { _, event ->
                val draggableView = event?.localState as View

                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        val clipDescription = event.clipDescription
                        clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    }

                    DragEvent.ACTION_DRAG_ENTERED -> {
                        Log.d("ItemTaskViewHolder", "View under drag: $adapterPosition")
                        Log.d(
                            "ItemTaskViewHolder",
                            "currAdapter: $tasksAdapter, lastAdapter: $currentVisibleAdapter"
                        )

                        isNewAdapter = currentVisibleAdapter != tasksAdapter
                        if (isNewAdapter) {
                            currentVisibleAdapter?.removeDataAt(ItemTaskViewHolder.oldPosition)
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

                        var newPos = 0
                        if (!isNewAdapter) {
                            if (event.y < pivot && !isMovedUp) {
                                isMovedDown = false
                                isMovedUp = true
                                newPos =
                                    if (adapterPosition < ItemTaskViewHolder.oldPosition) ItemTaskViewHolder.oldPosition - 1 else adapterPosition - 1
                                Log.d(
                                    "ItemTaskViewHolder",
                                    "oldPos: ${ItemTaskViewHolder.oldPosition}, newPos: $newPos"
                                )
                                tasksAdapter.move(ItemTaskViewHolder.oldPosition, newPos)
                            } else if (event.y > pivot && !isMovedDown) {
                                isMovedUp = false
                                isMovedDown = true
                                newPos =
                                    if (adapterPosition < ItemTaskViewHolder.oldPosition) adapterPosition + 1 else adapterPosition
                                Log.d(
                                    "ItemTaskViewHolder",
                                    "oldPos: $ItemTaskViewHolder.oldPosition, newPos: $newPos"
                                )
                                tasksAdapter.move(ItemTaskViewHolder.oldPosition, newPos)
                            }
                        } else if (!isInsertHandled) {
                            isInsertHandled = true
                            Log.d("ItemTaskViewHolder", "insertPos: $adapterPosition")
                            tasksAdapter.insertDragShadow(tasksAdapter, adapterPosition)
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

                        val data = event.clipData.getItemAt(0).text.toString()
                        Log.d("ItemTaskViewHolder", "Drop data: $data")

                        val moshi = Moshi.Builder().build()
                        val jsonAdapter = moshi.adapter(DragAndDropTaskItem::class.java)
                        val dragItem = jsonAdapter.fromJson(data) ?: throw NullPointerException("Data obtained from the ClipData is null")
                        val initPosition = dragItem.initPosition
                        val initAdapter = dragItem.initAdapter
                        val initTasks = dragItem.initTasksList
                        val task = dragItem.task


                        Log.d(
                            "ItemTaskViewHolder",
                            "ACTION_DROP: are adapters the same: ${initAdapter == tasksAdapter.toString()}"
                        )
                        if (initAdapter != tasksAdapter.toString()) {

                            Log.d("ItemTaskViewHolder", "ACTION_DROP: dropped task $task")

                            tasksAdapter.dropCallbacks.dropToInsert(
                                tasksAdapter,
                                initTasks.toList(),
                                task,
                                initPosition,
                                tasksAdapter.toPosition
                            )
                        } else {
                            tasksAdapter.dropCallbacks.dropToMove(
                                tasksAdapter,
                                initPosition,
                                tasksAdapter.toPosition
                            )
                        }
                        true
                    }

                    DragEvent.ACTION_DRAG_ENDED -> {
                        draggableView.visibility = View.VISIBLE
//                    if (!event.result) {
//                        false
//                    } else {
//                        true
//                    }
                        event.result
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