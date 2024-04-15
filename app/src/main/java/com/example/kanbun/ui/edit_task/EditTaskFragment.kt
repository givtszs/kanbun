package com.example.kanbun.ui.edit_task

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.kanbun.R
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.ui.create_task.CreateTaskFragmentArgs
import com.example.kanbun.ui.create_task.CreateTaskFragmentDirections
import com.example.kanbun.ui.create_task.CreateTaskViewModel
import com.example.kanbun.ui.task_editor.TaskEditorFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditTaskFragment : TaskEditorFragment(){

    companion object {
        private const val TAG = "EditTaskFragment"
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
                val updatedTask = createTask(args.task)
                viewModel.editTask(
                    oldTask = args.task!!,
                    updatedTask = updatedTask,
                    boardListInfo = boardListInfo
                ) {
                    navController.navigate(
                        EditTaskFragmentDirections.actionEditTaskFragmentToTaskDetailsFragment(
                            task = updatedTask,
                            boardList = args.boardList
                        )
                    )
                }
            }
        }
    }
}