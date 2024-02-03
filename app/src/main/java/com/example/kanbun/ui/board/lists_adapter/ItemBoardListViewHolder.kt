package com.example.kanbun.ui.board.lists_adapter

import android.util.Log
import android.view.DragEvent
import android.view.View
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.VERTICAL_SCROLL_DISTANCE
import com.example.kanbun.databinding.ItemBoardListBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.ui.board.DropCallbacks
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.model.BoardListInfo
import com.example.kanbun.ui.model.DragAndDropTaskItem
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "DragNDrop"

class ItemBoardListViewHolder(
    private val binding: ItemBoardListBinding,
    private val dropCallbacks: DropCallbacks,
    private val navController: NavController,
    private val coroutineScope: CoroutineScope,
    private val onCreateTaskListener: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val tasksAdapter: TasksAdapter
    private var scrollJob: Job? = null

    private val dragListener: (Int) -> View.OnDragListener = { scrollDistance ->
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

        tasksAdapter = TasksAdapter(dropCallbacks) { task ->
            // navigate to task settings fragment
//                navController.navigate()
        }

        Log.d("TasksAdapter", "ItemBoardListViewHolder init:\ttasksAdapter: $tasksAdapter")

        binding.rvTasks.adapter = tasksAdapter

        // set up drag listener
        binding.topSide.setOnDragListener(dragListener(-VERTICAL_SCROLL_DISTANCE))

        binding.bottomSide.setOnDragListener(dragListener(VERTICAL_SCROLL_DISTANCE))

        binding.rvTasks.setOnDragListener { view, event ->
            val draggableView = event.localState as View

            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true

                DragEvent.ACTION_DROP -> {
                    Log.d("ItemTaskViewHolder", "RecView#ACTION_DROP at $tasksAdapter")

                    val data = event.clipData.getItemAt(0).text.toString()
                    Log.d("ItemTaskViewHolder", "RecView#ACTION_DROP: clip data: $data")

                    val moshi = Moshi.Builder().build()
                    val jsonAdapter = moshi.adapter(DragAndDropTaskItem::class.java)
                    val dragItem = jsonAdapter.fromJson(data)

                    if (dragItem == null) {
                        Log.d(
                            "ItemTaskViewHolder",
                            "RecView#ACTION_DROP: dragItem is null"
                        )
                        false
                    } else {
                        tasksAdapter.tempRemovedTask = null
//                        Log.d(
//
//                            "ItemTaskViewHolder",
//                            "RecView#ACTION_DROP: are adapters the same: ${dragItem.initAdapter == tasksAdapter.toString()}"
//                        )
                        Log.d("ItemTaskViewHolder", "ACTION_DROP: isNewAdapter: ${tasksAdapter.isNewAdapter}, " +
                                "containsDragShadow: ${tasksAdapter.containsDragShadow}")
                        val containedDragShadow = tasksAdapter.removeDragShadow()

                        if (tasksAdapter.isNewAdapter || containedDragShadow) {
                            Log.d(
                                "ItemTaskViewHolder",
                                "RecView#ACTION_DROP: insert task ${dragItem.task}"
                            )

                            dropCallbacks.dropToInsert(
                                adapterToInsert = tasksAdapter,
                                dragItem,
                                TasksAdapter.ItemTaskViewHolder.oldPosition
                            )
                        } else {
                            Log.d(
                                "ItemTaskViewHolder",
                                "RecView#ACTION_DROP: move tasks from ${dragItem.initPosition} to ${TasksAdapter.ItemTaskViewHolder.oldPosition}"
                            )
                            dropCallbacks.dropToMove(
                                tasksAdapter,
                                dragItem.initPosition,
                                TasksAdapter.ItemTaskViewHolder.oldPosition
                            )
                        }
                        true
                    }
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    Log.d(
                        "ItemTaskViewHolder", "RecView#ACTION_DRAG_ENDED"
                    )
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