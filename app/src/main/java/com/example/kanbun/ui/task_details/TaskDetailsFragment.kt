package com.example.kanbun.ui.task_details

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
import androidx.navigation.fragment.navArgs
import com.example.kanbun.R
import com.example.kanbun.common.DATE_TIME_FORMAT
import com.example.kanbun.common.TaskAction
import com.example.kanbun.common.convertTimestampToDateString
import com.example.kanbun.databinding.FragmentTaskDetailsBinding
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.common_adapters.TagsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "TaskDetailsFragment"

@AndroidEntryPoint
class TaskDetailsFragment : BaseFragment(), StateHandler {
    private var _binding: FragmentTaskDetailsBinding? = null
    private val binding: FragmentTaskDetailsBinding get() = _binding!!
    private val viewModel: TaskDetailsViewModel by viewModels()
    private val args: TaskDetailsFragmentArgs by navArgs()
    private var tagsAdapter: TagsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.topAppBar.toolbar)
        loadSupplementaryInfo()
        collectState()
    }

    // TODO: Update this bullshit of a class to load the task details in a single repository request

    private fun loadSupplementaryInfo() {
        viewModel.getAuthor()
        viewModel.getTags(args.task.tags, args.boardListInfo.path)
        viewModel.getMembers()
    }

    override fun setUpListeners() {
        val task = args.task
        binding.apply {
            tvName.text = task.name
            tvDescription.text =
                task.description.ifEmpty { resources.getString(R.string.no_description) }

            tvDate.text = resources.getString(
                R.string.date_starts_ends,
                convertTimestampToDateString(DATE_TIME_FORMAT, task.dateStarts),
                convertTimestampToDateString(DATE_TIME_FORMAT, task.dateEnds)
            )

            fabEditTask.setOnClickListener {
                navController.navigate(
                    TaskDetailsFragmentDirections.actionTaskDetailsFragmentToCreateTaskFragment(
                        actionType = TaskAction.ACTION_EDIT,
                        task = task,
                        boardListInfo = args.boardListInfo
                    )
                )
            }
        }
    }

    override fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.taskDetailsState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.TaskDetailsViewState) {
            binding.apply {
                Log.d(TAG, "isLoading: $isLoading")
                loading.root.isVisible = isLoading

                tvCreatedBy.text = resources.getString(R.string.created_by, author.name)

                if (tags.isNotEmpty()) {
                    tvNoTags.visibility = View.GONE

                    tagsAdapter = TagsAdapter().also {
                        it.tags = tags
                    }

                    rvTags.apply {
                        adapter = tagsAdapter
                        visibility = View.VISIBLE
                    }
                } else {
                    tvNoTags.visibility = View.VISIBLE
                    rvTags.visibility = View.GONE
                }

                if (members.isNotEmpty()) {
                    tvNoMembers.visibility = View.GONE
                    // set up recyclerview
                    rvMembers.visibility = View.VISIBLE
                } else {
                    tvNoMembers.visibility = View.VISIBLE
                    rvMembers.visibility = View.GONE
                }
                tvMembersLabel.text = resources.getString(R.string.task_members, members.size)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tagsAdapter = null
        _binding = null
    }
}