package com.example.kanbun.ui.edit_task

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.kanbun.R
import com.example.kanbun.domain.model.TaskList
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.task_editor.TaskEditorFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditTaskFragment : TaskEditorFragment(){

    companion object {
        private const val TAG = "EditTaskFragment"
    }

    private val args: EditTaskFragmentArgs by navArgs()
    override val taskList: TaskList by lazy {
        args.taskList
    }
    override val task: Task by lazy {
        args.task
    }
    override val viewModel: EditTaskViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.topAppBar.toolbar, "Edit task")
    }

    override fun setUpListeners() {
        super.setUpListeners()
        binding.btnCreateTask.apply {
            isEnabled = true
            text = getString(R.string.edit_task)
            setOnClickListener {
                val updatedTask = createTask(task)
                viewModel.editTask(
                    oldTask = task,
                    updatedTask = updatedTask,
                    taskListInfo = taskListInfo
                ) {
                    navController.navigate(
                        EditTaskFragmentDirections.actionEditTaskFragmentToTaskDetailsFragment(
                            task = updatedTask,
                            taskList = taskList
                        )
                    )
                }
            }
        }
    }
}