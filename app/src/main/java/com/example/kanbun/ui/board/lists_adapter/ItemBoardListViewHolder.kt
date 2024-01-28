package com.example.kanbun.ui.board.lists_adapter

import android.content.ClipDescription
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.DragEvent
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemBoardListBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private const val TAG = "DragNDrop"

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

        tasksAdapter = TasksAdapter() { task ->
            // navigate to task settings fragment
//                navController.navigate()
        }

        Log.d("TasksAdapter", "ItemBoardListViewHolder init:\ttasksAdapter: $tasksAdapter")

        binding.rvTasks. apply {
            adapter = tasksAdapter

            val handler = Handler(Looper.getMainLooper())
            var rvWidth = 0
            var currentX = 0f
            val runnable = object : Runnable {
                override fun run() {
//                    Log.d("ItemBoardListVH", "${this@ItemBoardListViewHolder} currentX: $currentX")
//
//                    // Perform scrolling action based on the latest x value
//                    if (currentX < 10f) {
//                        // Scroll to the left
//                        scrollBy(-20, 0)
//                    } else if (currentX > rvWidth - 10f) {
//                        // Scroll to the right
//                        scrollBy(20, 0)
//                    }
//
//                    // Post the next scroll after a delay
//                    handler.postDelayed(this, 1)
                }
            }

            // set up drag listener
            setOnDragListener { view, event ->
                Log.d(TAG, "rvTasks.onDragListener is called")
                rvWidth = view.width
                val draggableItem = event.localState as View
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        val clipDescription = event.clipDescription
                        Log.d(TAG, "rvTasks.onDragListener#ACTION_DRAG_STARTED: clipDescr: ${clipDescription.getMimeType(0)}")
                        if (!clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                            false
                        } else {
                            handler.postDelayed(runnable, 1)
                            true
                        }
                    }

                    DragEvent.ACTION_DRAG_ENTERED -> {
                        Log.d(TAG, "rvTasks.onDragListener#ACTION_DRAG_ENTERED")
                        view.alpha = 0.3f
                        true
                    }

                    DragEvent.ACTION_DRAG_LOCATION -> {
                        Log.d(TAG, "rvTasks.onDragListener#ACTION_DRAG_LOCATION")
//                        Log.d("ItemBoardListVH", "x: ${event.x}")
//                        currentX = event.x
                        true
                    }

                    DragEvent.ACTION_DRAG_EXITED -> {
                        Log.d(TAG, "rvTasks.onDragListener#ACTION_DRAG_EXITED")
                        handler.removeCallbacks(runnable)
                        view.alpha = 1.0f
                        true
                    }

                    DragEvent.ACTION_DRAG_ENDED -> {
                        Log.d(TAG, "rvTasks.onDragListener#ACTION_DRAG_ENDED")
                        handler.removeCallbacks(runnable)
                        view.alpha = 1.0f
                        draggableItem.visibility = View.VISIBLE
                        true
                    }

                    else -> false
                }
            }
        }

    }

    fun bind(list: BoardList) {
        binding.apply {
            tvListName.text = list.name
        }

        Log.d("TasksAdapter", "bind:\ttasks: ${list.tasks}")

        tasksAdapter.setData(list.tasks)
    }
}