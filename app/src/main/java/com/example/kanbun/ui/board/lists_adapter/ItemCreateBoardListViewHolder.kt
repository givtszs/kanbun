package com.example.kanbun.ui.board.lists_adapter

import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemCreateBoardListBinding

class ItemCreateBoardListViewHolder(
    binding: ItemCreateBoardListBinding,
    private val onClickListener: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.cardView.setOnClickListener {
            onClickListener()
        }
    }
}