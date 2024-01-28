package com.example.kanbun.ui.board.tasks_adapter

import android.content.ClipData
import android.content.ClipDescription
import android.util.Log
import android.view.View
import android.view.View.DragShadowBuilder
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemTaskBinding
import com.example.kanbun.domain.model.Task

class ItemTaskViewHolder(
    private val binding: ItemTaskBinding,
    private val clickAtPosition: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private var task: Task? = null
    private val dragMessage = "Task ${task?.name} is being dragged"

    private val TAG = "DragNDrop"

    init {
        binding.materialCard.setOnClickListener {
            clickAtPosition(adapterPosition)
        }

        // set up on long click listener that initiates the dragging motion
        binding.materialCard.setOnLongClickListener { view ->
            Log.d(TAG, "onLongClickListener is called")

            val item = ClipData.Item(dragMessage)
            val dataToDrag = ClipData(
                dragMessage,
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item
            )
            val taskShadow = DragShadowBuilder(view)

            with(view) {
                startDragAndDrop(dataToDrag, taskShadow, view, 0)
                visibility = View.GONE
            }

            true
        }
    }

    fun bind(task: Task) {
        Log.d("TasksAdapter", "bind:\ttask: $task")
        binding.apply {
            tvName.text = task.name
        }
        this.task = task
    }
}