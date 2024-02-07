package com.example.kanbun.ui.create_task

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.kanbun.R
import com.example.kanbun.common.TaskAction
import com.example.kanbun.databinding.FragmentCreateTaskBinding
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "CreateTaskFragment"

@AndroidEntryPoint
class CreateTaskFragment : BaseFragment(), StateHandler {
    private var _binding: FragmentCreateTaskBinding? = null
    private val binding: FragmentCreateTaskBinding get() = _binding!!
    private val viewModel: CreateTaskViewModel by viewModels()
    private val args: CreateTaskFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (args.actionType) {
            TaskAction.ACTION_CREATE -> {
                setUpActionBar(binding.topAppBar.toolbar, "Create task")
            }

            TaskAction.ACTION_EDIT -> {
                setUpActionBar(binding.topAppBar.toolbar, "Edit task")
            }
        }
        collectState()
    }

    override fun setUpListeners() {
        when (args.actionType) {
            TaskAction.ACTION_CREATE -> {
                binding.apply {
                    etName.doOnTextChanged { _, _, _, count ->
                        Log.d(TAG, "etName: count: $count")
                        btnCreateTask.isEnabled = count > 0
                    }

                    btnCreateTask.setOnClickListener {
                        viewModel.createTask(
                            Task(
                                name = etName.text?.trim().toString(),
                                description = etDescription.text?.trim().toString(),
                                boardListInfo = args.task!!.boardListInfo,
                                author = viewModel.firebaseUser?.uid!!,
                            )
                        ) {
                            navController.popBackStack()
                        }
                    }
                }
            }

            TaskAction.ACTION_EDIT -> {
                val task = args.task
                viewModel.init(task)
                binding.apply {
                    etName.setText(task?.name)
                    etDescription.setText(task?.description)
                    with(btnCreateTask) {
                        isEnabled = true
                        setOnClickListener {
                            task?.let {
                                val updatedTask = task.copy(
                                    name = etName.text?.trim().toString(),
                                    description = etDescription.text?.trim().toString()
                                )
                                viewModel.editTask(updatedTask) {
                                    navController.navigate(
                                        CreateTaskFragmentDirections.actionCreateTaskFragmentToTaskDetailsFragment(
                                            updatedTask
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun collectState() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.createTaskState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.CreateTaskViewState) {
            binding.apply {
                if (isUpsertingTask) {
                    btnCreateTask.text = ""
                    btnCreateTask.isEnabled = false
                    loading.isVisible = true
                } else {
                    btnCreateTask.text = if (args.actionType == TaskAction.ACTION_CREATE) {
                        resources.getString(R.string.create_task)
                    } else {
                        resources.getString(R.string.save)
                    }
                    btnCreateTask.isEnabled =
                        etName.text?.trim().toString().isNotEmpty()
                    loading.isVisible = false
                }
            }

            message?.let {
                showToast(it)
                viewModel.messageShown()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}