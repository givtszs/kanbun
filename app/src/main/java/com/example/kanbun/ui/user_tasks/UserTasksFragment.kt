package com.example.kanbun.ui.user_tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.kanbun.R
import com.example.kanbun.databinding.FragmentUserTasksBinding
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.main_activity.MainActivity
import com.google.android.material.appbar.MaterialToolbar

class UserTasksFragment : BaseFragment() {
    private var _binding: FragmentUserTasksBinding? = null
    private val binding: FragmentUserTasksBinding get() = _binding!!
    private var tasksAdapter: TasksAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.topAppBar.toolbar)
    }

    override fun setUpActionBar(toolbar: MaterialToolbar) {
        (requireActivity() as MainActivity).setSupportActionBar(toolbar)
        toolbar.title = "Tasks"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        tasksAdapter = null
    }

    override fun setUpListeners() {
        tasksAdapter = TasksAdapter { task ->
            // open the task details fragment in its board
            showToast("Clicked on task ${task.name}")
        }
    }
}