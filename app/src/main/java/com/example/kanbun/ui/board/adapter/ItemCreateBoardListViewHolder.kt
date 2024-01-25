package com.example.kanbun.ui.board.adapter

import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemCreateBoardListBinding

class ItemCreateBoardListViewHolder(
    private val binding: ItemCreateBoardListBinding,
    private val onClickListener: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind() {
        binding.root.setOnClickListener {
            onClickListener()
        }
    }
}