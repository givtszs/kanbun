package com.example.kanbun.ui.board.tasks_adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemTaskBinding
import com.example.kanbun.domain.model.Task

class TasksAdapter(
    private val onTaskClickListener: (Task) -> Unit
) : RecyclerView.Adapter<TasksAdapter.ItemTaskViewHolder>() {

    private var tasks: List<Task> = emptyList()

    fun setData(data: List<Task>) {
        tasks = data
        notifyDataSetChanged()
    }

    class ItemTaskViewHolder(
        private val binding: ItemTaskBinding,
        private val clickAtPosition: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.materialCard.setOnClickListener {
                clickAtPosition(adapterPosition)
            }
        }

        fun bind(task: Task) {
            binding.apply {
                tvName.text = task.name
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTaskViewHolder {
        return ItemTaskViewHolder(
             ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        ) { position ->
            onTaskClickListener(tasks[position])
        }
    }

    override fun onBindViewHolder(holder: ItemTaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size
}