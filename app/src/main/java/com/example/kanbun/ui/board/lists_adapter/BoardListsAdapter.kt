package com.example.kanbun.ui.board.lists_adapter

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
    private val taskDropCallback: TaskDropCallbacks,
    val boardListDropCallback: DropCallback,
    private val onCreateListClickListener: () -> Unit,
    private val onCreateTaskListener: (BoardList) -> Unit,
    private val loadingCompleteCallback: () -> Unit,
    private val onTaskClicked: (Task, BoardListInfo) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var boardTags: List<Tag> = emptyList()
    var lists: List<BoardList> = emptyList()

    fun setData(data: List<BoardList>) {
        lists = data
        notifyDataSetChanged()
        loadingCompleteCallback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            BoardListsAdapterViewType.VIEW_TYPE_LIST ->  ItemBoardListViewHolder(
                binding = ItemBoardListBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                taskDropCallback = taskDropCallback,
                boardListDropCallback = boardListDropCallback,
                coroutineScope = coroutineScope,
                boardListAdapter = this@BoardListsAdapter,
                onCreateTaskListener = { position ->
                    onCreateTaskListener(lists[position])
                },
                onTaskClicked = onTaskClicked
            )

            BoardListsAdapterViewType.VIEW_TYPE_CREATE_LIST -> ItemCreateBoardListViewHolder(
                ItemCreateBoardListBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onCreateListClickListener
            )
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
        when (holder) {
            is ItemBoardListViewHolder -> holder.bind(lists[position])
            is ItemCreateBoardListViewHolder -> holder.bind()
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