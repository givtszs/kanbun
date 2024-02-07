package com.example.kanbun.ui.create_task

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.example.kanbun.common.TaskAction
import com.example.kanbun.databinding.FragmentCreateTaskBinding
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.BaseFragment
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

private const val TAG = "CreateTaskFragment"

@AndroidEntryPoint
class CreateTaskFragment : BaseFragment() {
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
    }

    override fun setUpListeners() {
        when (args.actionType) {
            TaskAction.ACTION_CREATE -> {
                binding.apply {
                    etName.doOnTextChanged { text, _, _, count ->
                        Log.d(TAG, "etName: count: $count")
                        btnCreateTask.isEnabled = count > 0
                    }

                    btnCreateTask.setOnClickListener {
                        lifecycleScope.launch {
                            btnCreateTask.text = ""
                            btnCreateTask.isEnabled = false
                            loading.isVisible = true
                            viewModel.createTask(
                                args.task!!.copy(
                                    name = etName.text?.trim().toString(),
                                    description = etDescription.text?.trim().toString(),
                                    boardListInfo = args.task!!.boardListInfo,
                                    author = viewModel.firebaseUser?.uid!!,
                                )
                            )
                            navController.popBackStack()
                        }
                    }
                }
            }

            TaskAction.ACTION_EDIT -> {
                binding.apply {
                    btnCreateTask.apply {
                        text = "Save changes"
                        setOnClickListener {

                        }
                    }
                }
            }
        }
    }
}