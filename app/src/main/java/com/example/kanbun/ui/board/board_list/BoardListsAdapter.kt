package com.example.kanbun.ui.board.board_list

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.BoardListsAdapterViewType
import com.example.kanbun.databinding.ItemBoardListBinding
import com.example.kanbun.databinding.ItemCreateBoardListBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.board.DropCallback
import com.example.kanbun.ui.board.TaskDropCallbacks
import kotlinx.coroutines.CoroutineScope

class BoardListsAdapter(
    private val coroutineScope: CoroutineScope,
    private val taskDropCallbacks: TaskDropCallbacks,
    val boardListDropCallback: DropCallback,
    private val callbacks: BoardListsAdapterCallbacks,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var boardTags: List<Tag> = emptyList()
    var lists: List<BoardList> = emptyList()

    fun setData(data: List<BoardList>) {
        lists = data
        notifyDataSetChanged()
        callbacks.loadingComplete()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            BoardListsAdapterViewType.VIEW_TYPE_LIST -> ItemBoardListViewHolder(
                binding = ItemBoardListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                coroutineScope = coroutineScope,
                taskDropCallbacks = taskDropCallbacks,
                boardListAdapter = this@BoardListsAdapter,
                callbacks = object : BoardListViewHolderCallbacks {
                    override fun createTask(position: Int) {
                        callbacks.createTask(lists[position])
                    }

                    override fun onTaskClicked(task: Task, boardListInfo: BoardListInfo) {
                        callbacks.onTaskClicked(task, boardListInfo)
                    }

                    override fun openMenu(position: Int) {
                        callbacks.onBoardListMenuClicked(lists[position])
                    }
                }
            )

            BoardListsAdapterViewType.VIEW_TYPE_CREATE_LIST -> ItemCreateBoardListViewHolder(
                ItemCreateBoardListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
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
            BoardListsAdapterViewType.VIEW_TYPE_LIST
        } else {
            BoardListsAdapterViewType.VIEW_TYPE_CREATE_LIST
        }
    }

    override fun getItemCount(): Int = lists.size + 1 // 1 for the anchor `Create list` layout
}