package com.example.kanbun.ui.board.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.BoardListsAdapterViewType
import com.example.kanbun.databinding.ItemBoardListBinding
import com.example.kanbun.databinding.ItemCreateBoardListBinding
import com.example.kanbun.domain.model.BoardList

class BoardListsAdapter(
    private val onCreateListClickListener: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var lists: List<BoardList> = emptyList()

    fun setData(data: List<BoardList>) {
        lists = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            BoardListsAdapterViewType.VIEW_TYPE_LIST ->  ItemBoardListViewHolder(
                ItemBoardListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )

            BoardListsAdapterViewType.VIEW_TYPE_CREATE_LIST -> ItemCreateBoardListViewHolder(
                ItemCreateBoardListBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onCreateListClickListener
            )
            else -> throw IllegalArgumentException("Invalid view type")
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