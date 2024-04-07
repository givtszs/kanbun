package com.example.kanbun.ui.board.board_list

import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemCreateBoardListBinding

class ItemCreateBoardListViewHolder(
    binding: ItemCreateBoardListBinding,
    isWorkspaceAdminOrBoardMember: Boolean,
    private val onClickListener: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.cardView.setOnClickListener {
            onClickListener()
        }
        binding.cardView.isEnabled = isWorkspaceAdminOrBoardMember
    }
}