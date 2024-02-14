package com.example.kanbun.ui.board.tasks_adapter

import android.content.ClipData
import android.content.ClipDescription
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.moshi
import com.example.kanbun.databinding.ItemTaskBinding
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.board.TaskDropCallbacks
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.ui.model.DragAndDropTaskItem
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
    val taskDropCallbacks: TaskDropCallbacks,
    private val onTaskClicked: (Task) -> Unit,
) : RecyclerView.Adapter<ItemTaskViewHolder>() {
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
                    ItemTaskViewHolder.TaskDragAndDropHelper.handleDrop(
                        event.clipData,
                        taskDropCallbacks
                    )
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    Log.d("ItemTaskViewHolder", "RecView#ACTION_DRAG_ENDED")
                    ItemTaskViewHolder.TaskDragAndDropHelper.removeDropZone()
                    true
                }

                else -> false
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
}