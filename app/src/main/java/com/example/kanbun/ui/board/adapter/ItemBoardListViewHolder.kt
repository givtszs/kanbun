package com.example.kanbun.ui.board.adapter

import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemBoardListBinding
import com.example.kanbun.domain.model.BoardList

class ItemBoardListViewHolder(
        private val binding: ItemBoardListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(list: BoardList) {
            binding.apply {
                tvListName.text = list.name
            }
        }
    }