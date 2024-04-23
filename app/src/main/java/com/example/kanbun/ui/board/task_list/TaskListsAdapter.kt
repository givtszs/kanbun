package com.example.kanbun.ui.board.task_list

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.Role
import com.example.kanbun.common.TAG
import com.example.kanbun.databinding.ItemCreateTaskListBinding
import com.example.kanbun.databinding.ItemTaskListBinding
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.TaskList
import com.example.kanbun.ui.board.BoardFragment
import com.example.kanbun.ui.board.DropCallback
import com.example.kanbun.ui.board.TaskDropCallbacks
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.user_boards.UserBoardsFragment
import kotlinx.coroutines.CoroutineScope

class TaskListsAdapter(
    private val parent: ViewGroup,
    private val coroutineScope: CoroutineScope,
    private val taskDropCallbacks: TaskDropCallbacks,
    val taskListDropCallback: DropCallback,
    private val callbacks: TaskListsAdapterCallbacks,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_BOARD_LIST = 0
        private const val VIEW_TYPE_CREATE_LIST = 1
    }

    private var preInflatedViews: Iterator<ItemTaskListBinding> = emptyList<ItemTaskListBinding>().iterator()

    init {
        val layoutInflater = LayoutInflater.from(parent.context)
        preInflatedViews = List(10) {
            ItemTaskListBinding.inflate(layoutInflater, parent, false)
        }.iterator()
    }

    var boardTags: List<Tag> = emptyList()
        set(value) {
            if (field != value) {
                field = value
            }
        }

    var tasksAdapters: Iterator<TasksAdapter> = emptyList<TasksAdapter>().iterator()

    var lists: List<TaskList> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                tasksAdapters = value.map { taskList ->
                    TasksAdapter(
                        parent = parent,
                        taskDropCallbacks = taskDropCallbacks,
                        onTaskClicked = { task ->
                            callbacks.onTaskClicked(task, taskList)
                        },
                        loadTaskTags = { tagIds ->
                            boardTags.filter { tag -> tag.id in tagIds }
                        }
                    )
                }.iterator()
                notifyDataSetChanged()
                callbacks.loadingComplete()
            }
        }

    private val isWorkspaceAdminOrBoardMember get() = UserBoardsFragment.workspaceRole == Role.Workspace.Admin || BoardFragment.isBoardMember
    private val taskListViewHolderCallbacks = object : TaskListViewHolderCallbacks {
        override fun createTask(position: Int) {
            callbacks.createTask(lists[position])
        }

        override fun onTaskClicked(task: Task, taskList: TaskList) {
            callbacks.onTaskClicked(task, taskList)
        }

        override fun openMenu(position: Int) {
            callbacks.onTaskListMenuClicked(
                lists[position],
                lists,
                isWorkspaceAdminOrBoardMember
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_BOARD_LIST -> {
                val binding = if (preInflatedViews.hasNext()) {
                    Log.d(TAG, "onCreateViewHolder: use pre inflated view")
                    preInflatedViews.next()
                } else {
                    ItemTaskListBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                }
                ItemTaskListViewHolder(
                    binding = binding,
                    isWorkspaceAdminOrBoardMember = isWorkspaceAdminOrBoardMember,
                    coroutineScope = coroutineScope,
                    taskDropCallbacks = taskDropCallbacks,
                    taskListsAdapter = this@TaskListsAdapter,
                    callbacks = taskListViewHolderCallbacks
                )

            }
            VIEW_TYPE_CREATE_LIST -> ItemCreateTaskListViewHolder(
                binding = ItemCreateTaskListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                isWorkspaceAdminOrBoardMember = isWorkspaceAdminOrBoardMember
            ) {
                callbacks.createTaskList()
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    fun move(from: Int, to: Int) {
        if (from != to && to != -1) {
            notifyItemMoved(from, to)
            ItemTaskListViewHolder.oldPosition = to
            Log.d("ItemTaskListViewHolder", "Move from $from to $to")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemTaskListViewHolder) {
            holder.bind(lists[position])
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < lists.size) {
            VIEW_TYPE_BOARD_LIST
        } else {
            VIEW_TYPE_CREATE_LIST
        }
    }

    override fun getItemCount(): Int = lists.size + 1 // 1 for the anchor `Create list` layout
}