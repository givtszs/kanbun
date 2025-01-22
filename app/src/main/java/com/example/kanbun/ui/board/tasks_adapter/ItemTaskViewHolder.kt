package com.example.kanbun.ui.board.tasks_adapter

import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.DATE_TIME_FORMAT
import com.example.kanbun.common.TAG
import com.example.kanbun.common.convertTimestampToDateString
import com.example.kanbun.databinding.ItemTaskBinding
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.custom_views.TagView

class ItemTaskViewHolder(
    isWorkspaceAdminOrBoardMember: Boolean,
    private val binding: ItemTaskBinding,
    private val tasksAdapter: TasksAdapter,
    private val clickAtPosition: (Int) -> Unit,
    private val loadTags: ((List<String>) -> List<Tag>)?
) : RecyclerView.ViewHolder(binding.root) {

    private var task: Task? = null

    init {
        binding.taskCard.setOnClickListener {
            clickAtPosition(adapterPosition)
        }

        if (isWorkspaceAdminOrBoardMember && tasksAdapter.taskDropCallbacks != null) {
            // initiates dragging action
            binding.taskCard.setOnLongClickListener { _ ->
                Log.d(TAG, "long clicked perform at: $adapterPosition")
                TaskDragAndDropHelper.startDrag(
                    adapterPosition,
                    tasksAdapter,
                    binding.taskCard,
                    task
                )
            }
        }

        binding.taskCard.setOnDragListener { receiverView, event ->
            TaskDragAndDropHelper.taskCardViewDragEventHandler(
                tasksAdapter,
                receiverView,
                event,
                adapterPosition,
            )
        }

        binding.taskCard.dropArea.setOnDragListener { view, event ->
            TaskDragAndDropHelper.dropAreaViewDragEventHandler(
                event,
                view
            )
        }
    }

    fun bind(task: Task) {
        Log.d("TasksAdapter", "bind:\ttask: $task")
        this.task = task

        binding.apply {
            taskCard.task.isVisible = task.id != TaskDragAndDropHelper.DROP_ZONE_TASK
            taskCard.dropArea.isVisible = task.id == TaskDragAndDropHelper.DROP_ZONE_TASK

            setUpTagsFlexbox(task)

            taskCard.taskName.text = task.name
            taskCard.taskName.layoutParams = (taskCard.taskName.layoutParams as ConstraintLayout.LayoutParams).apply {
                topMargin = if (taskCard.taskTags.visibility == View.GONE) {
                    0
                } else {
                    taskCard.taskTags.resources.getDimensionPixelSize(R.dimen.task_name_top_margin)
                }
            }

            taskCard.taskDescription.isVisible = task.description.isNotEmpty()
            taskCard.taskDescription.text = task.description

            // set up date
            getDisplayDate(task.dateStarts, task.dateEnds).also { date ->
                taskCard.taskDate.text = date
                taskCard.taskDate.isVisible = date != null
                taskCard.taskHorSeparator.isVisible = date != null
            }
        }
    }

    private fun getDisplayDate(dateStarts: Long?, dateEnds: Long?): String? {
        return when {
            dateStarts != null && dateEnds != null ->
                itemView.resources.getString(
                    R.string.task_date,
                    convertTimestampToDateString(DATE_TIME_FORMAT, dateStarts),
                    convertTimestampToDateString(DATE_TIME_FORMAT, dateEnds)
                )

            dateStarts != null ->
                itemView.resources.getString(
                    R.string.date_starts,
                    convertTimestampToDateString(
                        DATE_TIME_FORMAT,
                        dateStarts
                    )
                )

            dateEnds != null ->
                itemView.resources.getString(
                    R.string.date_ends,
                    convertTimestampToDateString(
                        DATE_TIME_FORMAT,
                        dateEnds
                    )
                )

            else -> null
        }
    }

    private fun setUpTagsFlexbox(task: Task) {

        val tags = loadTags?.invoke(task.tags)
//        Log.d(TAG, "tags: $tags")

        if (tags.isNullOrEmpty()) {
            binding.taskCard.taskTags.isVisible = false
            return
        }

        binding.apply {
            taskCard.taskTags.isVisible = true
            taskCard.taskTags.removeAllViews()

            tags.forEach { tag ->
                val tagView = TagView(context = itemView.context, isBig = false).also {
                    it.bind(tag, isClickable = false, isSelected = false)
                }
                taskCard.taskTags.addView(tagView)
            }
        }
    }
}