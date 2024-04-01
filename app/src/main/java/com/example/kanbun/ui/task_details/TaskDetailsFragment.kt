package com.example.kanbun.ui.task_details

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
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
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Task
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
    private lateinit var task: Task
    private lateinit var boardList: BoardList

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
        task = args.task
        boardList = args.boardList

        setUpActionBar(binding.topAppBar.toolbar)
        setUpOptionsMenu()
        loadSupplementaryInfo()
        collectState()
    }

    // TODO: Update this bullshit of a class to load the task details in a single repository request

    private fun loadSupplementaryInfo() {
        viewModel.getAuthor(task.author)
        viewModel.getTags(
            task = task,
            boardListId = boardList.id,
            boardListPath = boardList.path
        )
        viewModel.getMembers()
    }

    override fun setUpListeners() {
        val task = args.task
        binding.apply {
            tvName.text = task.name
            tvDescription.text =
                task.description.ifEmpty { resources.getString(R.string.no_description) }

            tvDate.text = resources.getString(
                R.string.task_date,
                convertTimestampToDateString(DATE_TIME_FORMAT, task.dateStarts),
                convertTimestampToDateString(DATE_TIME_FORMAT, task.dateEnds)
            )

            fabEditTask.setOnClickListener {
                navController.navigate(
                    TaskDetailsFragmentDirections.actionTaskDetailsFragmentToCreateTaskFragment(
                        actionType = TaskAction.ACTION_EDIT,
                        task = task,
                        boardList = args.boardList
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
                tvMembersLabel.text = resources.getString(R.string.task_members_count, members.size)

                message?.let {
                    showToast(it)
                    viewModel.messageShown()
                }
            }
        }
    }

    private fun setUpOptionsMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            @SuppressLint("RestrictedApi")
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.task_details_menu, menu)
                if (menu is MenuBuilder) {
                    // display icon in the options menu list
                    menu.setOptionalIconsVisible(true)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_item_delete -> {
                        viewModel.deleteTask(
                            taskPosition = args.task.position.toInt(),
                            boardList = args.boardList,
                            navigateOnDelete = { navController.popBackStack() }
                        )
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tagsAdapter = null
        _binding = null
    }
}