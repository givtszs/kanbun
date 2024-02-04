package com.example.kanbun.ui.board.lists_adapter

import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Point
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.view.View.DragShadowBuilder
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.VERTICAL_SCROLL_DISTANCE
import com.example.kanbun.databinding.ItemBoardListBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.ui.board.DropCallback
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.model.BoardListInfo
import com.example.kanbun.ui.model.DragAndDropListItem
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "DragNDrop"

class ItemBoardListViewHolder(
    private val binding: ItemBoardListBinding,
    private val taskDropCallback: DropCallback,
    private val boardListDropCallback: DropCallback,
    private val navController: NavController,
    private val coroutineScope: CoroutineScope,
    private val boardListAdapter: BoardListsAdapter,
    private val onCreateTaskListener: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        var oldPosition = -1
        var isActionDragEndedHandled = false
    }

    private val tasksAdapter: TasksAdapter
    private var scrollJob: Job? = null
    private var boardList: BoardList? = null

    private val rvScrollerDragListener: (Int) -> View.OnDragListener = { scrollDistance ->
        View.OnDragListener { _, event ->
            val draggableView = event?.localState as View

            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
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

        tasksAdapter = TasksAdapter(taskDropCallback) { task ->
            // navigate to task settings fragment
//                navController.navigate()
        }

        Log.d("TasksAdapter", "ItemBoardListViewHolder init:\ttasksAdapter: $tasksAdapter")

        binding.rvTasks.adapter = tasksAdapter

        // set up drag listener
        binding.topSide.setOnDragListener(rvScrollerDragListener(-VERTICAL_SCROLL_DISTANCE))

        binding.bottomSide.setOnDragListener(rvScrollerDragListener(VERTICAL_SCROLL_DISTANCE))

        binding.rvTasks.setOnDragListener { _, event ->
            val draggableView = event.localState as View

            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }

                DragEvent.ACTION_DROP -> {
                    Log.d("ItemTaskViewHolder", "RecView#ACTION_DROP at $tasksAdapter")
                    taskDropCallback.drop(
                        clipData = event.clipData,
                        adapter = tasksAdapter,
                        position = TasksAdapter.ItemTaskViewHolder.oldPosition
                    )
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    Log.d("ItemTaskViewHolder", "RecView#ACTION_DRAG_ENDED")
                    tasksAdapter.dragCallbacks.removeDropZone()
                    true
                }

                else -> false
            }
        }

        binding.tvListName.setOnLongClickListener { view ->
            val moshi = Moshi.Builder().build()
            val jsonAdapter: JsonAdapter<DragAndDropListItem> =
                moshi.adapter(DragAndDropListItem::class.java)
            val json = jsonAdapter.toJson(DragAndDropListItem(initPosition = adapterPosition))

            ItemBoardListViewHolder.oldPosition = adapterPosition

            val item = ClipData.Item(json)
            val clipData = ClipData(
                "list_json",
                arrayOf(ClipDescription.MIMETYPE_TEXT_HTML), // use MIMETYPE other than in task onLongClickListener
                item
            )
            val taskShadow = object : DragShadowBuilder(binding.listCard) {
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
                taskShadow,
                binding.listCard,
                0
            )
            if (isStartSuccess) {
                binding.listCard.visibility = View.INVISIBLE
            }

            true
        }

        var isMoveLeft = false
        var isMoveRight = false

        binding.listCard.setOnDragListener { _, event ->
            val draggableView = event.localState as View
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    Log.d("ItemBoardListViewHolder", "ACTION_DRAG_STARTED")
                    isActionDragEndedHandled = false
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    val half = draggableView.width / 2
                    val leftPivot = half - half / 2
                    val rightPivot = half + half / 2

                    if (event.x < leftPivot && !isMoveLeft) {
                        isMoveLeft = true
                        isMoveRight = false
                        val newPos =
                            if (adapterPosition < ItemBoardListViewHolder.oldPosition) adapterPosition else adapterPosition - 1
                        Log.d("ItemBoardListViewHolder", "ACTION_DRAG_LOCATION: newPos: $newPos")
                        boardListAdapter.move(ItemBoardListViewHolder.oldPosition, newPos)
                    } else if (event.x > rightPivot && !isMoveRight) {
                        isMoveRight = true
                        isMoveLeft = false
                        val newPos =
                            if (adapterPosition < ItemBoardListViewHolder.oldPosition) adapterPosition + 1 else adapterPosition
                        Log.d("ItemBoardListViewHolder", "ACTION_DRAG_LOCATION: newPos: $newPos")
                        boardListAdapter.move(ItemBoardListViewHolder.oldPosition, newPos)
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
                    boardListDropCallback.drop(
                        clipData = event.clipData,
                        adapter = boardListAdapter,
                        position = ItemBoardListViewHolder.oldPosition
                    )
                    true
                }

                else -> false
            }
        }

        binding.materialCard.setOnDragListener { _, event ->
            val draggableView = event.localState as View
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED ->
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)

                DragEvent.ACTION_DROP -> {
                    Log.d("ItemBoardListViewHolder", "MaterialCard#ACTION_DROP")
                    boardListDropCallback.drop(
                        clipData = event.clipData,
                        adapter = boardListAdapter,
                        position = ItemBoardListViewHolder.oldPosition
                    )
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    if (!isActionDragEndedHandled) {
                        draggableView.visibility = View.VISIBLE
                        if (!event.result) {
                            Log.d("ItemBoardListViewHolder", "MaterialCard#ACTION_DRAG_ENDED: event.result: ${event.result}")
                            // reset items position if dragging failed
                            boardListAdapter.notifyDataSetChanged()
                        }
                        isActionDragEndedHandled = true
                    }
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

        boardList = list
    }
}