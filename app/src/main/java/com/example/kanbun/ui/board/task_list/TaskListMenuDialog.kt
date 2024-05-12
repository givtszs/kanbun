package com.example.kanbun.ui.board.task_list

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import com.example.kanbun.R
import com.example.kanbun.databinding.TaskListMenuDialogBinding
import com.example.kanbun.domain.model.TaskList
import com.example.kanbun.ui.buildTextInputDialog
import com.example.kanbun.ui.buildDeleteConfirmationDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaskListMenuDialog : BottomSheetDialogFragment() {
    private var _binding: TaskListMenuDialogBinding? = null
    private val binding: TaskListMenuDialogBinding get() = _binding!!
    private val viewModel: TaskListViewModel by viewModels()
    private lateinit var taskList: TaskList
    private lateinit var taskLists: List<TaskList>
    private var areOptionsEnabled: Boolean = false

    companion object {
        fun init(
            taskList: TaskList,
            taskLists: List<TaskList>,
            areOptionsEnabled: Boolean
        ): TaskListMenuDialog {
            return TaskListMenuDialog().apply {
                this.taskList = taskList
                this.taskLists = taskLists
                this.areOptionsEnabled = areOptionsEnabled
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TaskListMenuDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            btnDelete.isEnabled = areOptionsEnabled
            btnDelete.setOnClickListener {
                buildDeleteConfirmationDialog(
                    requireContext(),
                    R.string.delete_task_list_dialog_title
                ) {
                    viewModel.deleteTaskList(taskList, taskLists) { dismiss() }
                }.show()
            }

            btnEditName.isEnabled = areOptionsEnabled
            btnEditName.setOnClickListener {
                buildTextInputDialog(
                    context = requireContext(),
                    item = R.string.task_list,
                    title = R.string.edit_task_list_name
                ) { text ->
                    viewModel.editTaskListName(
                        newName = text,
                        taskListPath = taskList.path,
                        taskListId = taskList.id
                    ) {
                        this@TaskListMenuDialog.dismiss()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}