package com.example.kanbun.ui.board.lists_adapter

import android.util.Log
import android.view.DragEvent
import android.view.View
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.VERTICAL_SCROLL_DISTANCE
import com.example.kanbun.databinding.ItemBoardListBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.ui.board.DropCallback
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.model.BoardListInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "DragNDrop"

class ItemBoardListViewHolder(
    private val binding: ItemBoardListBinding,
    private val dropCallback: DropCallback,
    private val navController: NavController,
    private val coroutineScope: CoroutineScope,
    private val onCreateTaskListener: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val tasksAdapter: TasksAdapter
    private var scrollJob: Job? = null

    private val rvScrollerDragListener: (Int) -> View.OnDragListener = { scrollDistance ->
        View.OnDragListener { _, event ->
            val draggableView = event?.localState as View

            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    true
                }

                DragEvent.ACTION_DRAG_ENTERED -> {
                    scrollJob = coroutineScope.launch {
                        while (true) {
                            binding.rvTasks.scrollBy(0, scrollDistance)
                            delay(1)
                        }
                    }
                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    scrollJob?.cancel()
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    scrollJob?.cancel()
                    true
                }

                else -> false
            }
        }
    }

    init {
        binding.btnCreateTask.setOnClickListener {
            onCreateTaskListener(adapterPosition)
        }

        tasksAdapter = TasksAdapter(dropCallback) { task ->
            // navigate to task settings fragment
//                navController.navigate()
        }

        Log.d("TasksAdapter", "ItemBoardListViewHolder init:\ttasksAdapter: $tasksAdapter")

        binding.rvTasks.adapter = tasksAdapter

        // set up drag listener
        binding.topSide.setOnDragListener(rvScrollerDragListener(-VERTICAL_SCROLL_DISTANCE))

        binding.bottomSide.setOnDragListener(rvScrollerDragListener(VERTICAL_SCROLL_DISTANCE))

        binding.rvTasks.setOnDragListener { view, event ->
            val draggableView = event.localState as View

            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true

                DragEvent.ACTION_DROP -> {
                    Log.d("ItemTaskViewHolder", "RecView#ACTION_DROP at $tasksAdapter")
                    dropCallback.drop(
                        clipData = event.clipData,
                        adapter = tasksAdapter,
                        position = TasksAdapter.ItemTaskViewHolder.oldPosition
                    )
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    Log.d("ItemTaskViewHolder", "RecView#ACTION_DRAG_ENDED")
                    tasksAdapter.removeDragShadow()
                    true
                }

                else -> false
            }
        }
    }

    fun bind(list: BoardList) {
        binding.apply {
            tvListName.text = list.name
        }

        Log.d("TasksAdapter", "bind:\ttasks: ${list.tasks}")

        tasksAdapter.setData(list.tasks)
        tasksAdapter.listInfo = BoardListInfo(list.id, list.path)
    }
}