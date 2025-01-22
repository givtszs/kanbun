package com.example.kanbun.ui.board.task_list

import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemCreateTaskListBinding

class ItemCreateTaskListViewHolder(
    private val binding: ItemCreateTaskListBinding,
    private val onClickListener: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.cardView.setOnClickListener {
            onClickListener()
        }
    }

    fun bind(isWorkspaceAdminOrBoardMember: Boolean) {
        binding.cardView.isEnabled = isWorkspaceAdminOrBoardMember
    }

}