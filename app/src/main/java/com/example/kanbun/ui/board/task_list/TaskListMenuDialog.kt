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
                viewModel.deleteTaskList(taskList, taskLists) { dismiss() }
            }

            btnEditName.isEnabled = areOptionsEnabled
            btnEditName.setOnClickListener {
                buildEditNameDialog()
            }
        }
    }

    private fun buildEditNameDialog() {
        val editTextName = EditText(requireContext()).apply {
            setText(taskList.name)
            hint = "Enter list's name"

            // Create layout parameters
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // Set margins (adjust these values as needed)
                val horizontalMarginInPixels =
                    resources.getDimensionPixelSize(R.dimen.alert_dialog_edit_text_horizontal_margin)
                setMargins(horizontalMarginInPixels, 0, horizontalMarginInPixels, 0)
            }

            // Apply layout parameters to the EditText
            this.layoutParams = layoutParams
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit list's name")
            .setView(editTextName)
            .setPositiveButton("Save") { _, _ ->
                viewModel.editTaskListName(
                    newName = editTextName.text.trim().toString(),
                    taskListPath = taskList.path,
                    taskListId = taskList.id
                ) {
                    this@TaskListMenuDialog.dismiss()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                dismiss()
            }
            .create()
            .apply {
                setOnShowListener {
                    val positiveButton = getButton(AlertDialog.BUTTON_POSITIVE)
                    editTextName.doOnTextChanged { text, _, _, _ ->
                        positiveButton.isEnabled = text.isNullOrEmpty() == false
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}