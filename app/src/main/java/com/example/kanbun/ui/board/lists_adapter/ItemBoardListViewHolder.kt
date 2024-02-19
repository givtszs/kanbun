package com.example.kanbun.ui.board.lists_adapter

import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Point
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.view.View.DragShadowBuilder
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.moshi
import com.example.kanbun.databinding.ItemBoardListBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.ui.board.TaskDropCallbacks
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.model.DragAndDropListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ItemBoardListViewHolder(
    coroutineScope: CoroutineScope,
    taskDropCallbacks: TaskDropCallbacks,
    val binding: ItemBoardListBinding,
    private val boardListAdapter: BoardListsAdapter,
    private val callbacks: BoardListViewHolderCallbacks
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        var oldPosition = -1
        var isActionDragEndedHandled = false

        /** Holds the reference to the coroutine performing recycler view scrolling **/
        private var scrollJob: Job? = null
        private lateinit var coroutineScope: CoroutineScope

        fun scrollTasksRecyclerView(binding: ItemBoardListBinding, scrollDistance: Int) {
            scrollJob = coroutineScope.launch {
                while (true) {
                    binding.rvTasks.scrollBy(0, scrollDistance)
                    delay(1)
                }
            }
        }
    }

    private val tasksAdapter: TasksAdapter
    private var boardList: BoardList? = null

    init {
        ItemBoardListViewHolder.coroutineScope = coroutineScope

        binding.btnCreateTask.setOnClickListener {
            callbacks.onCreateTask(adapterPosition)
        }

        tasksAdapter = TasksAdapter(
            boardListAdapter.boardTags,
            taskDropCallbacks,
            onTaskClicked = { task ->
                callbacks.onTaskClicked(task, BoardListInfo(boardList!!.id, boardList!!.path))
            }
        )

        Log.d("TasksAdapter", "ItemBoardListViewHolder init:\ttasksAdapter: $tasksAdapter")
        binding.rvTasks.adapter = tasksAdapter

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
        binding.tvListName.setOnLongClickListener { view ->
            ListDragAndDropHelper.currentAdapter = boardListAdapter
            ListDragAndDropHelper.startListDragging(
                view = view,
                position = adapterPosition,
                binding = binding
            )
        }

        // handle drag events when the user drops the draggable view on the board list item
        binding.listCard.setOnDragListener { view, event ->
            ListDragAndDropHelper.handleDragEvent(view, event, adapterPosition)
        }

        // use `materialCard` (parent view to the `listCard` drag shadow) instead of the board lists recycler view
        //  to handle drop events performed outside of the `listCard` view
        binding.materialCard.setOnDragListener { _, event ->
            ListDragAndDropHelper.handleDragEvent(event)
        }
    }

    fun bind(list: BoardList) {
        binding.apply {
            tvListName.text = list.name
        }

        Log.d("TasksAdapter", "bind:\ttasks: ${list.tasks}")

        tasksAdapter.setData(list.tasks)
        tasksAdapter.listInfo = BoardListInfo(list.id, list.path)

        boardList = list
    }

    private object ListDragAndDropHelper {

        const val VERTICAL_SCROLL_DISTANCE = 5
        lateinit var currentAdapter: BoardListsAdapter

        fun rvScrollerDragListener(
            binding: ItemBoardListBinding,
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

        fun startListDragging(view: View, position: Int, binding: ItemBoardListBinding): Boolean {
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
            val boardListShadow = object : DragShadowBuilder(binding.listCard) {
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
                boardListShadow,
                binding.listCard,
                0
            )

            if (isStartSuccess) {
                binding.listCard.visibility = View.INVISIBLE
            }

            return isStartSuccess
        }

        private var isMoveLeft = false
        private var isMoveRight = false

        fun handleDragEvent(view: View, event: DragEvent, position: Int): Boolean {
            return when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    Log.d("ItemBoardListViewHolder", "ACTION_DRAG_STARTED")
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
                        Log.d("ItemBoardListViewHolder", "ACTION_DRAG_LOCATION: newPos: $newPos")
                        currentAdapter.move(oldPosition, newPos)
                    } else if (event.x > rightPivot && !isMoveRight) {
                        isMoveRight = true
                        isMoveLeft = false
                        val newPos =
                            if (position < oldPosition) position + 1 else position
                        Log.d("ItemBoardListViewHolder", "ACTION_DRAG_LOCATION: newPos: $newPos")
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
                    Log.d("ItemBoardListViewHolder", "ACTION_DROP")
                    currentAdapter.boardListDropCallback.drop(
                        clipData = event.clipData,
                        adapter = currentAdapter,
                        position = oldPosition
                    )
                    true
                }

                else -> false
            }
        }

        fun handleDragEvent(event: DragEvent): Boolean {
            val draggableView = event.localState as View
            return when (event.action) {
                DragEvent.ACTION_DRAG_STARTED ->
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)

                DragEvent.ACTION_DROP -> {
                    Log.d("ItemBoardListViewHolder", "MaterialCard#ACTION_DROP")
                    currentAdapter.boardListDropCallback.drop(
                        clipData = event.clipData,
                        adapter = currentAdapter,
                        position = ItemBoardListViewHolder.oldPosition
                    )
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    if (!isActionDragEndedHandled) {
                        draggableView.visibility = View.VISIBLE
                        if (!event.result) {
                            Log.d(
                                "ItemBoardListViewHolder",
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