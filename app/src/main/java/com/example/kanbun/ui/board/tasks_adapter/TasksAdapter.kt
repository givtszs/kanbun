package com.example.kanbun.ui.board.tasks_adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.Role
import com.example.kanbun.databinding.ItemTaskBinding
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.TaskListInfo
import com.example.kanbun.ui.board.BoardFragment
import com.example.kanbun.ui.board.TaskDropCallbacks
import com.example.kanbun.ui.user_boards.UserBoardsFragment

/**
 * A [RecyclerView.Adapter] responsible for managing and displaying a list of [Task]s
 *
 * @property taskDropCallbacks the set of callbacks used when the dragged item gets dropped
 * @property onTaskClicked the callback called when the user click on a task item
 * @property loadTaskTags the callback to load tags of a task
 */
class TasksAdapter(
    private val onTaskClicked: (Task) -> Unit,
) : RecyclerView.Adapter<ItemTaskViewHolder>() {

    var taskDropCallbacks: TaskDropCallbacks? = null
        private set
    private var loadTaskTags: ((List<String>) -> List<Tag>)? = null

    constructor(
        taskDropCallbacks: TaskDropCallbacks,
        onTaskClicked: (Task) -> Unit,
        loadTaskTags: (List<String>) -> List<Tag>
    ) : this(onTaskClicked = onTaskClicked) {
        this.taskDropCallbacks = taskDropCallbacks
        this.loadTaskTags = loadTaskTags
    }

    var tasks: MutableList<Task> = mutableListOf()
        private set

    lateinit var listInfo: TaskListInfo

    private val isWorkspaceAdminOrBoardMember
        get() =
            UserBoardsFragment.workspaceRole == Role.Workspace.Admin || BoardFragment.isBoardMember

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
        return ItemTaskViewHolder(
            binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            tasksAdapter = this@TasksAdapter,
            clickAtPosition = { position ->
                onTaskClicked(tasks[position])
            },
            loadTags = loadTaskTags,
            isWorkspaceAdminOrBoardMember = isWorkspaceAdminOrBoardMember
        )
    }

    override fun onBindViewHolder(holder: ItemTaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size
}