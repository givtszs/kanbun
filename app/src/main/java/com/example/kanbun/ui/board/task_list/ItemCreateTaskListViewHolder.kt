package com.example.kanbun.ui.board.task_list

import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemCreateTaskListBinding

class ItemCreateTaskListViewHolder(
    binding: ItemCreateTaskListBinding,
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