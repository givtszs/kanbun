package com.example.kanbun.ui.board.lists_adapter

import android.util.Log
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemBoardListBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter

class ItemBoardListViewHolder(
    private val binding: ItemBoardListBinding,
    private val navController: NavController,
    private val onCreateTaskListener: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    private val tasksAdapter: TasksAdapter

    init {
        binding.btnCreateTask.setOnClickListener {
            onCreateTaskListener(adapterPosition)
        }

        tasksAdapter = TasksAdapter { task ->
            // navigate to task settings fragment
//                navController.navigate()
        }

        Log.d("TasksAdapter", "ItemBoardListViewHolder init:\ttasksAdapter: $tasksAdapter")

        binding.rvTasks.adapter = tasksAdapter
    }

    fun bind(list: BoardList) {
        binding.apply {
            tvListName.text = list.name
        }

        Log.d("TasksAdapter", "bind:\ttasks: ${list.tasks}")

        tasksAdapter.setData(list.tasks)
    }
}