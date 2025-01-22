package com.example.kanbun.ui.user_tasks

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.kanbun.common.TAG
import com.example.kanbun.databinding.FragmentUserTasksBinding
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.main_activity.MainActivity
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserTasksFragment : BaseFragment(), StateHandler {
    private var _binding: FragmentUserTasksBinding? = null
    private val binding: FragmentUserTasksBinding get() = _binding!!
    private val viewModel: UserTasksViewModel by viewModels()
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
        collectState()
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
        binding.rvTasks.adapter = tasksAdapter
    }

    override fun collectState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userBoardsState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.UserTasksViewState) {
            Log.d(this@UserTasksFragment.TAG, "processState: state: $this")
            tasksAdapter?.setData(tasks)
            binding.loading.root.isVisible = isLoading
            message?.let {
                showToast(it)
                viewModel.messageShown()
            }
        }
    }
}