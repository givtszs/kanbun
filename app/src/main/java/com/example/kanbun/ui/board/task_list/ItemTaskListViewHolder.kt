package com.example.kanbun.ui.board.task_list

import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Point
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.view.View.DragShadowBuilder
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.TAG
import com.example.kanbun.common.moshi
import com.example.kanbun.databinding.ItemTaskListBinding
import com.example.kanbun.domain.model.TaskList
import com.example.kanbun.domain.model.TaskListInfo
import com.example.kanbun.ui.board.TaskDropCallbacks
import com.example.kanbun.ui.board.tasks_adapter.ItemTaskViewHolder
import com.example.kanbun.ui.board.tasks_adapter.TaskDragAndDropHelper
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.model.DragAndDropListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ItemTaskListViewHolder(
    coroutineScope: CoroutineScope,
    isWorkspaceAdminOrBoardMember: Boolean,
    taskDropCallbacks: TaskDropCallbacks,
    val binding: ItemTaskListBinding,
    private val taskListsAdapter: TaskListsAdapter,
    private val callbacks: TaskListViewHolderCallbacks
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        var oldPosition = -1
        var isActionDragEndedHandled = false

        /** Holds the reference to the coroutine performing recycler view scrolling **/
        private var scrollJob: Job? = null
        private lateinit var coroutineScope: CoroutineScope

        private fun scrollTasksRecyclerView(binding: ItemTaskListBinding, scrollDistance: Int) {
            scrollJob = coroutineScope.launch {
                while (true) {
                    binding.rvTasks.scrollBy(0, scrollDistance)
                    delay(1)
                }
            }
        }
    }

    private val tasksAdapter: TasksAdapter
    private var taskList: TaskList? = null

    init {
        ItemTaskListViewHolder.coroutineScope = coroutineScope

        binding.btnCreateTask.isEnabled = isWorkspaceAdminOrBoardMember
        binding.btnCreateTask.setOnClickListener {
            callbacks.createTask(adapterPosition)
        }

        binding.btnMore.setOnClickListener {
            callbacks.openMenu(adapterPosition)
        }

        tasksAdapter = if (taskListsAdapter.tasksAdapters.hasNext()) {
            val adapter = taskListsAdapter.tasksAdapters.next()
            Log.d(TAG, "init: task adapter $adapter is obtained from tasksAdapters")
            adapter
        } else {
            TasksAdapter(
                parent = binding.rvTasks,
                taskDropCallbacks = taskDropCallbacks,
                onTaskClicked = { task ->
                    callbacks.onTaskClicked(task, taskList!!)
                },
                loadTaskTags = { tagIds ->
                    taskListsAdapter.boardTags.filter { tag -> tag.id in tagIds }
                }
            )
        }


        Log.d("ItemTaskViewHolder", "tasksAdapter: $tasksAdapter")
        binding.rvTasks.adapter = tasksAdapter
        binding.rvTasks.setOnDragListener { view, event ->
            TaskDragAndDropHelper.taskListDragEventHandler(
                tasksAdapter,
                view,
                event
            )
        }

        // scrolls the tasks RV to the top
        binding.topSide.setOnDragListener(
            ListDragAndDropHelper.rvScrollerDragListener(
                binding,
                -ListDragAndDropHelper.VERTICAL_SCROLL_DISTANCE
            )
        )

        // scrolls the tasks RV to the bottom
        binding.bottomSide.setOnDragListener(
            ListDragAndDropHelper.rvScrollerDragListener(
                binding,
                ListDragAndDropHelper.VERTICAL_SCROLL_DISTANCE
            )
        )

        // starts the drag and drop action
        if (isWorkspaceAdminOrBoardMember) {
            binding.tvListName.setOnLongClickListener { view ->
                ListDragAndDropHelper.currentAdapter = taskListsAdapter
                ListDragAndDropHelper.startListDragging(
                    view = view,
                    position = adapterPosition,
                    draggableView = binding.listCard,
                    dropArea = binding.taskListDropArea
                )
            }
        }

        // handle drag events when the user drops the draggable view on the task list item
        binding.listCard.setOnDragListener { view, event ->
            ListDragAndDropHelper.handleDragEvent(view, event, adapterPosition)
        }

        binding.taskListDropArea.setOnDragListener { _, event ->
            ListDragAndDropHelper.handleDragEvent(event, binding.taskListDropArea)
        }
    }

    fun bind(list: TaskList) {
        binding.apply {
            tvListName.text = list.name
        }

        Log.d("TasksAdapter", "bind:\ttasks: ${list.tasks}")

        tasksAdapter.setData(list.tasks)
        tasksAdapter.listInfo = TaskListInfo(list.id, list.path)

        taskList = list
        binding.rvTasks.setItemViewCacheSize(list.tasks.size)
    }

    private object ListDragAndDropHelper {

        const val VERTICAL_SCROLL_DISTANCE = 5
        lateinit var currentAdapter: TaskListsAdapter

        fun rvScrollerDragListener(
            binding: ItemTaskListBinding,
            scrollDistance: Int
        ): View.OnDragListener {
            return View.OnDragListener { _, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    }

                    DragEvent.ACTION_DRAG_ENTERED -> {
                        scrollTasksRecyclerView(binding, scrollDistance)
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

        fun startListDragging(
            view: View,
            position: Int,
            draggableView: View,
            dropArea: View
        ): Boolean {
            oldPosition = position
            isActionDragEndedHandled = false

            val json = moshi.adapter(DragAndDropListItem::class.java)
                .toJson(DragAndDropListItem(initPosition = position))
            val item = ClipData.Item(json)
            val clipData = ClipData(
                "list_json",
                // use MIMETYPE other than the one used in the ItemTaskViewHolder onLongClickListener
                // to let the drag listeners distinguish between the items being dragged (task or board list)
                arrayOf(ClipDescription.MIMETYPE_TEXT_HTML),
                item
            )
            val taskListShadow = object : DragShadowBuilder(draggableView) {
                override fun onProvideShadowMetrics(
                    outShadowSize: Point?,
                    outShadowTouchPoint: Point?
                ) {
                    outShadowSize?.set(this.view.width, this.view.height)
                    if (outShadowSize != null) {
                        outShadowTouchPoint?.set(outShadowSize.x / 2, 100)
                    }
                }
            }
            val isStartSuccess = view.startDragAndDrop(
                clipData,
                taskListShadow,
                draggableView,
                0
            )

            if (isStartSuccess) {
                draggableView.visibility = View.INVISIBLE
                dropArea.visibility = View.VISIBLE
            }

            return isStartSuccess
        }

        private var isMoveLeft = false
        private var isMoveRight = false

        fun handleDragEvent(view: View, event: DragEvent, position: Int): Boolean {
            return when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    Log.d(TAG, "ACTION_DRAG_STARTED")
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    val half = view.width / 2
                    val leftPivot = half - half / 2
                    val rightPivot = half + half / 2

                    if (event.x < leftPivot && !isMoveLeft) {
                        isMoveLeft = true
                        isMoveRight = false
                        val newPos =
                            if (position < oldPosition) position else position - 1
                        Log.d(TAG, "ACTION_DRAG_LOCATION: newPos: $newPos")
                        currentAdapter.move(oldPosition, newPos)
                    } else if (event.x > rightPivot && !isMoveRight) {
                        isMoveRight = true
                        isMoveLeft = false
                        val newPos =
                            if (position < oldPosition) position + 1 else position
                        Log.d(TAG, "ACTION_DRAG_LOCATION: newPos: $newPos")
                        currentAdapter.move(oldPosition, newPos)
                    }

                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    isMoveLeft = false
                    isMoveRight = false
                    true
                }

                DragEvent.ACTION_DROP -> {
                    Log.d(TAG, "ACTION_DROP")
                    currentAdapter.taskListDropCallback.drop(
                        clipData = event.clipData,
                        adapter = currentAdapter,
                        position = oldPosition
                    )
                    true
                }

                else -> false
            }
        }

        fun handleDragEvent(event: DragEvent, dropArea: View): Boolean {
            val draggableView = event.localState as View
            return when (event.action) {
                DragEvent.ACTION_DRAG_STARTED ->
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)

                DragEvent.ACTION_DROP -> {
                    Log.d(TAG, "MaterialCard#ACTION_DROP")
                    currentAdapter.taskListDropCallback.drop(
                        clipData = event.clipData,
                        adapter = currentAdapter,
                        position = ItemTaskListViewHolder.oldPosition
                    )
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    if (!isActionDragEndedHandled) {
                        draggableView.visibility = View.VISIBLE
                        dropArea.visibility = View.GONE
                        if (!event.result) {
                            Log.d(
                                TAG,
                                "MaterialCard#ACTION_DRAG_ENDED: event.result: ${event.result}"
                            )
                            // reset items position if dragging failed
                            currentAdapter.notifyDataSetChanged()
                        }
                        isActionDragEndedHandled = true
                    }
                    true
                }

                else -> false
            }
        }
    }
}