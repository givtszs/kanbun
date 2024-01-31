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

                DragEvent.ACTION_DRAG_ENTERED -> {
                    Log.d(
                        "ItemTaskViewHolder",
                        "RecView#ACTION_DRAG_ENTERED: \nAdapter: $tasksAdapter"
                    )
                    true
                }

                DragEvent.ACTION_DROP -> {
                    Log.d("ItemTaskViewHolder", "RecView#ACTION_DROP at $tasksAdapter")

                    val data = event.clipData.getItemAt(0).text.toString()
                    Log.d("ItemTaskViewHolder", "Drop data: $data")

                    val moshi = Moshi.Builder().build()
                    val jsonAdapter = moshi.adapter(DragAndDropTaskItem::class.java)
                    val dragItem = jsonAdapter.fromJson(data) ?: throw NullPointerException("Data obtained from the ClipData is null")
                    val initPosition = dragItem.initPosition
                    val initAdapter = dragItem.initAdapter
                    val initTasks = dragItem.initTasksList
                    val task = dragItem.task


                    Log.d(
                        "ItemTaskViewHolder",
                        "RecView#ACTION_DROP: are adapters the same: ${initAdapter == tasksAdapter.toString()}"
                    )
                    if (initAdapter != tasksAdapter.toString()) {

                        Log.d("ItemTaskViewHolder", "RecView#ACTION_DROP: dropped task $task")

                        dropCallbacks.dropToInsert(
                            adapter = tasksAdapter,
                            tasksToRemoveFrom = initTasks.toList(),
                            task,
                            initPosition,
                            tasksAdapter.toPosition
                        )
                    } else {
                        dropCallbacks.dropToMove(
                            tasksAdapter,
                            initPosition,
                            tasksAdapter.toPosition
                        )
                    }

                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    Log.d(
                        "ItemTaskViewHolder",
                        "RecView#ACTION_DRAG_EXITED: \nAdapter: $tasksAdapter"
                    )
//                    TasksAdapter.adapterOfDraggedView?.removeDataAt(TasksAdapter.oldPosition)
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    Log.d(
                        "ItemTaskViewHolder",
                        "RecView#ACTION_DRAG_ENDED: result: ${event.result}"
                    )
                    draggableView.visibility = View.VISIBLE
                    if (!event.result) {
                        tasksAdapter.removeDragShadow()
                        false
                    } else {
                        true
                    }
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