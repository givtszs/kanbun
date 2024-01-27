package com.example.kanbun.ui.board.lists_adapter

import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemBoardListBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter

class ItemBoardListViewHolder(
    private val binding: ItemBoardListBinding,
    private val navController: NavController
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(list: BoardList) {
        binding.apply {
            tvListName.text = list.name

            val tasksAdapter = TasksAdapter { task ->
                // navigate to task settings fragment
//                navController.navigate()
            }

            binding.rvTasks.adapter = tasksAdapter
        }
    }
}