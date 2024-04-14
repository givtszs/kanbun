package com.example.kanbun.ui.board.board_list

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.Role
import com.example.kanbun.databinding.ItemBoardListBinding
import com.example.kanbun.databinding.ItemCreateBoardListBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.board.BoardFragment
import com.example.kanbun.ui.board.DropCallback
import com.example.kanbun.ui.board.TaskDropCallbacks
import com.example.kanbun.ui.user_boards.UserBoardsFragment
import kotlinx.coroutines.CoroutineScope

class BoardListsAdapter(
    private val coroutineScope: CoroutineScope,
    private val taskDropCallbacks: TaskDropCallbacks,
    val boardListDropCallback: DropCallback,
    private val callbacks: BoardListsAdapterCallbacks,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_BOARD_LIST = 0
        private const val VIEW_TYPE_CREATE_LIST = 1
    }

    var boardTags: List<Tag> = emptyList()
        set(value) {
            if (field != value) {
                field = value
            }
        }

    var lists: List<BoardList> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
                callbacks.loadingComplete()
            }
        }

    private val isWorkspaceAdminOrBoardMember get() = UserBoardsFragment.userRole == Role.Workspace.Admin || BoardFragment.isBoardMember

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_BOARD_LIST -> ItemBoardListViewHolder(
                binding = ItemBoardListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                isWorkspaceAdminOrBoardMember = isWorkspaceAdminOrBoardMember,
                coroutineScope = coroutineScope,
                taskDropCallbacks = taskDropCallbacks,
                boardListAdapter = this@BoardListsAdapter,
                callbacks = object : BoardListViewHolderCallbacks {
                    override fun createTask(position: Int) {
                        callbacks.createTask(lists[position])
                    }

                    override fun onTaskClicked(task: Task, boardList: BoardList) {
                        callbacks.onTaskClicked(task, boardList)
                    }

                    override fun openMenu(position: Int) {
                        callbacks.onBoardListMenuClicked(
                            lists[position],
                            lists,
                            isWorkspaceAdminOrBoardMember
                        )
                    }
                }
            )

            VIEW_TYPE_CREATE_LIST -> ItemCreateBoardListViewHolder(
                binding = ItemCreateBoardListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                isWorkspaceAdminOrBoardMember = isWorkspaceAdminOrBoardMember
            ) {
                callbacks.createBoardList()
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    fun move(from: Int, to: Int) {
        if (from != to && to != -1) {
            notifyItemMoved(from, to)
            ItemBoardListViewHolder.oldPosition = to
            Log.d("ItemBoardListViewHolder", "Move from $from to $to")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemBoardListViewHolder) {
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