package com.example.kanbun.ui.create_task

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

private const val TAG = "CreateTaskFragment"

@AndroidEntryPoint
class CreateTaskFragment : TaskEditorFragment(){
    private val args: CreateTaskFragmentArgs by navArgs()
    override val taskList: TaskList by lazy {
        args.taskList
    }
    override val task: Task? by lazy {
        args.task
    }
    override val viewModel: CreateTaskViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.toolbar, "Create task")
    }

    override fun setUpListeners() {
        super.setUpListeners()
        binding.btnCreateTask.text = getString(R.string.create_task)
        binding.btnCreateTask.setOnClickListener {
            viewModel.createTask(
                createTask(),
                taskList
            ) {
                navController.popBackStack()
            }
        }
    }
}
