package com.example.kanbun.ui.board.lists_adapter

import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemCreateBoardListBinding

class ItemCreateBoardListViewHolder(
    private val binding: ItemCreateBoardListBinding,
    private val onClickListener: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind() {
        binding.cardView.setOnClickListener {
            onClickListener()
        }
    }
}