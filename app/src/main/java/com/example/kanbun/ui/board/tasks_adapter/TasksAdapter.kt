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
import com.example.kanbun.ui.model.DragAndDropTaskItem
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import java.util.Collections
import kotlin.coroutines.coroutineContext

private var taskOldPosition = -1
private lateinit var draggedViewAdapter: TasksAdapter
private var currentAdapter: TasksAdapter? = null
private var draggedTask: Pair<Int, Task>? = null

class TasksAdapter(
    private val onTaskClickListener: (Task) -> Unit
) : RecyclerView.Adapter<TasksAdapter.ItemTaskViewHolder>() {

//    companion object {
//        var taskOldPosition = -1
//        var adapterOfDraggedView: TasksAdapter? = null
//        var draggedTask: Pair<Int, Task>? = null
//    }

    var tasks: MutableList<Task> = mutableListOf()

    fun setData(data: List<Task>) {
        tasks = data.toMutableList()
        Log.d("TasksAdapter", "adapter: ${this}\tsetData: $data")
        notifyDataSetChanged()
    }

    private val dragCallbackMove: (Int, Int) -> Unit = { from, to ->
        if (from != to) {
            notifyItemMoved(from, to)
//            Collections.swap(tasks, to, from)
            Log.d("ItemTaskViewHolder", "moved item from $from to $to")
            taskOldPosition = to
        }
    }

    fun removeDragShadow() {
        val shadowTask = tasks.find { it.id == "drag_shadow" }

        if (shadowTask != null && draggedTask != null) {
            removeDataAt(taskOldPosition)
            draggedViewAdapter.tasks.add(draggedTask!!.first, draggedTask!!.second)
            draggedViewAdapter.notifyItemInserted(draggedTask!!.first)
            Log.d("ItemTaskViewHolder", "Drag shadow task removed. Dragged task reset")
        }
    }

    fun removeDataAt(position: Int) {
        if (position != -1) {
            tasks.removeAt(taskOldPosition)
            notifyItemRemoved(position)
            Log.d("ItemTaskViewHolder", "removed item at $position")
//            taskOldPosition = -1
        }
    }

    val dragCallbackDrop: (TasksAdapter, Task) -> Unit = { adapter, task ->
        adapter.setData(
            tasks.map {
                if (it.id == "drag_shadow") {
                    task
                } else {
                    it
                }
            }
        )
        val updTasks = buildString {
            tasks.forEach {
                append("${it.id}, ")
            }
        }
        Log.d("ItemTaskViewHolder", "Updated tasks on drop: $updTasks")
        adapter.notifyDataSetChanged()
    }

    private val dragCallbackInsertDragShadow: (TasksAdapter, Int) -> Unit = { adapter, pos ->
        tasks.add(pos, Task(id = "drag_shadow"))
        val updTasks = buildString {
            tasks.forEach {
                append("${it.id}, ")
            }
        }
        Log.d("ItemTaskViewHolder", "Updated tasks: $updTasks")
        adapter.notifyItemInserted(pos)
        Log.d("ItemTaskViewHolder", "inserted item at $pos")
        taskOldPosition = pos
        currentAdapter = adapter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTaskViewHolder {
        Log.d("TasksAdapter", "onCreateViewHolder is called")
        return ItemTaskViewHolder(
//            tasksAdapter = this,
            binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false),
//            dragCallbackMove = dragCallbackMove,
//            dragCallbackInsert = dragCallbackInsert,
//            dragCallbackDrop = dragCallbackDrop,
        ) { position ->
            onTaskClickListener(tasks[position])
        }
    }

    override fun onBindViewHolder(holder: ItemTaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    inner class ItemTaskViewHolder(
//        private val tasksAdapter: TasksAdapter,
        private val binding: ItemTaskBinding,
//        private val dragCallbackMove: (Int, Int) -> Unit,
//        private val dragCallbackInsert: (TasksAdapter, Int) -> Unit,
//        private val dragCallbackDrop: (TasksAdapter, Task) -> Unit,
        private val clickAtPosition: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var task: Task? = null
        private val tasksAdapter = this@TasksAdapter

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
                taskOldPosition = adapterPosition
                currentAdapter = tasksAdapter
                draggedViewAdapter = tasksAdapter
                draggedTask = Pair(
                    adapterPosition,
                    task ?: throw NullPointerException("Task is null")
                )

                val moshi = Moshi.Builder().build()
                val jsonAdapter: JsonAdapter<DragAndDropTaskItem> =
                    moshi.adapter(DragAndDropTaskItem::class.java)
                val json =
                    jsonAdapter.toJson(DragAndDropTaskItem(task, tasksAdapter.toString()))
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

            var isTopHandled = false
            var isBottomHandled = false
            var isInsertHandled = false
            var isNewAdapter = false

            binding.materialCard.setOnDragListener { view, event ->
                val draggableView = event?.localState as View

                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        true
                    }

                    DragEvent.ACTION_DRAG_ENTERED -> {
                        Log.d("ItemTaskViewHolder", "View under drag: $adapterPosition")
                        Log.d(
                            "ItemTaskViewHolder",
                            "currAdapter: $tasksAdapter, lastAdapter: $currentAdapter"
                        )

                        isNewAdapter = currentAdapter != tasksAdapter
                        if (isNewAdapter) {
                            currentAdapter?.removeDataAt(taskOldPosition)
                        }
                        true
                    }

                    DragEvent.ACTION_DRAG_LOCATION -> {
                        val pivot = draggableView.height / 2

                        var newPos = 0
                        if (!isNewAdapter) {
                            if (event.y < pivot && !isTopHandled) {
                                isBottomHandled = false
                                isTopHandled = true
                                newPos =
                                    if (adapterPosition < taskOldPosition) taskOldPosition - 1 else adapterPosition - 1
                                Log.d(
                                    "ItemTaskViewHolder",
                                    "oldPos: $taskOldPosition, newPos: $newPos"
                                )
                                dragCallbackMove(taskOldPosition, newPos)
                            } else if (event.y > pivot && !isBottomHandled) {
                                isTopHandled = false
                                isBottomHandled = true
                                newPos =
                                    if (adapterPosition < taskOldPosition) adapterPosition + 1 else adapterPosition
                                Log.d(
                                    "ItemTaskViewHolder",
                                    "oldPos: $taskOldPosition, newPos: $newPos"
                                )
                                dragCallbackMove(taskOldPosition, newPos)
                            }
                        }

                        if (isNewAdapter && !isInsertHandled) {
                            isInsertHandled = true
                            Log.d("ItemTaskViewHolder", "insertPos: $adapterPosition")
                            dragCallbackInsertDragShadow(tasksAdapter, adapterPosition)
                        }

                        true
                    }

                    DragEvent.ACTION_DROP -> {
                        Log.d("ItemTaskViewHolder", "ACTION_DROP")

                        val data = event.clipData.getItemAt(0).text.toString()
                        Log.d("ItemTaskViewHolder", "Drop data: $data")

                        val moshi = Moshi.Builder().build()
                        val jsonAdapter = moshi.adapter(DragAndDropTaskItem::class.java)
                        val dragItem = jsonAdapter.fromJson(data)
                        val initAdapter = dragItem?.initAdapter

                        Log.d(
                            "ItemTaskViewHolder",
                            "Dragged over rvTasks, are adapters the same: ${initAdapter == tasksAdapter.toString()}"
                        )
                        if (initAdapter != tasksAdapter.toString()) {
                            val task = dragItem?.task
                                ?: throw NullPointerException("Task obtained from the ClipData is null")
                            Log.d("ItemTaskViewHolder", "Converted json: $task")

                            tasksAdapter.dragCallbackDrop(tasksAdapter, task)
//                        draggableView.visibility = View.VISIBLE
                            true
                        } else {
//                        draggableView.visibility = View.VISIBLE
                            false
                        }
                    }

                    DragEvent.ACTION_DRAG_EXITED -> {
                        isTopHandled = false
                        isBottomHandled = false
                        isInsertHandled = false
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