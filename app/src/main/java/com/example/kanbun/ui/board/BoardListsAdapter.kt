package com.example.kanbun.ui.board

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemBoardListBinding
import com.example.kanbun.domain.model.BoardList

class BoardListsAdapter() : RecyclerView.Adapter<BoardListsAdapter.ItemBoardListViewHolder>() {

    class ItemBoardListViewHolder(
        private val binding: ItemBoardListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(list: BoardList) {
            binding.apply {
                tvListName.text = list.name
            }
        }
    }

    private var lists: List<BoardList> = emptyList()

    fun setData(data: List<BoardList>) {
        lists = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemBoardListViewHolder {
        return ItemBoardListViewHolder(
            ItemBoardListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ItemBoardListViewHolder, position: Int) {
        holder.bind(lists[position])
    }

    override fun getItemCount(): Int = lists.size
}