package com.example.kanbun.ui.board.tasks_adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemTaskBinding
import com.example.kanbun.domain.model.Task

class TasksAdapter(
    private val onTaskClickListener: (Task) -> Unit
) : RecyclerView.Adapter<ItemTaskViewHolder>() {

    private var tasks: List<Task> = emptyList()

    fun setData(data: List<Task>) {
        tasks = data
        Log.d("TasksAdapter", "adapter: ${this}\tsetData: $data")
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTaskViewHolder {
        Log.d("TasksAdapter", "onCreateViewHolder is called")
        return ItemTaskViewHolder(
             ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        ) { position ->
            onTaskClickListener(tasks[position])
        }
    }

    override fun onBindViewHolder(holder: ItemTaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size
}