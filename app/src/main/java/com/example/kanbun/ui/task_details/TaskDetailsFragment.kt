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
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.kanbun.R
import com.example.kanbun.common.DATE_TIME_FORMAT
import com.example.kanbun.common.Role
import com.example.kanbun.common.convertTimestampToDateString
import com.example.kanbun.databinding.FragmentTaskDetailsBinding
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.BoardFragment
import com.example.kanbun.ui.board.common_adapters.TagsAdapter
import com.example.kanbun.ui.model.TagUi
import com.example.kanbun.ui.shared.SharedBoardViewModel
import com.example.kanbun.ui.user_boards.UserBoardsFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "TaskDetailsFragment"

@AndroidEntryPoint
class TaskDetailsFragment : BaseFragment(), StateHandler {
    private var _binding: FragmentTaskDetailsBinding? = null
    private val binding: FragmentTaskDetailsBinding get() = _binding!!

    private val taskDetailsViewModel: TaskDetailsViewModel by viewModels()
    private val sharedViewModel: SharedBoardViewModel by hiltNavGraphViewModels(R.id.board_graph)

    private val args: TaskDetailsFragmentArgs by navArgs()
    private val task: Task by lazy {
        args.task
    }

    private val isWorkspaceAdminOrBoardMember =
        UserBoardsFragment.userRole == Role.Workspace.Admin || BoardFragment.isBoardMember
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
        setUpOptionsMenu()
        setUpTagsAdapter()
        collectState()
    }

    override fun setUpListeners() {
        val task = args.task
        binding.apply {
            tvName.text = task.name
            tvDescription.text = task.description.ifEmpty {
                resources.getString(R.string.no_description)
            }


            val author = sharedViewModel.boardMembers.find { it.id == task.author }
            if (author != null) {
                tvCreatedBy.text = resources.getString(R.string.created_by, author.name)
            } else {
                tvCreatedBy.text = resources.getString(R.string.created_by, "")
                taskDetailsViewModel.getAuthor(task.author)
            }


            task.tags.let {
                rvTags.isVisible = it.isNotEmpty()
                tvNoTags.isVisible = it.isEmpty()
            }

            tvDate.text = resources.getString(
                R.string.task_date,
                convertTimestampToDateString(DATE_TIME_FORMAT, task.dateStarts),
                convertTimestampToDateString(DATE_TIME_FORMAT, task.dateEnds)
            )

            fabEditTask.isVisible = isWorkspaceAdminOrBoardMember
            fabEditTask.setOnClickListener {
                navController.navigate(
                    TaskDetailsFragmentDirections.actionTaskDetailsFragmentToEditTaskFragment(
                        boardList = args.boardList,
                        task = task
                    )
                )
            }
        }
    }

    private fun setUpTagsAdapter() {
        val tagsUi = sharedViewModel.tags.filter { it.id in task.tags }.map { TagUi(it, false) }
        tagsAdapter = TagsAdapter(areItemsClickable = false).apply {
            tags = tagsUi
        }
        binding.rvTags.adapter = tagsAdapter
    }

    private fun setUpMembersAdapter() {
        val members = sharedViewModel.boardMembers.filter { it.id in task.members }
        // initialize the members adapter
    }

    override fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskDetailsViewModel.taskDetailsState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.TaskDetailsViewState) {
            Log.d(TAG, "processState: $this")
            author?.let {
                binding.tvCreatedBy.text = resources.getString(R.string.created_by, it.name)
            }

            message?.let {
                showToast(it)
                taskDetailsViewModel.messageShown()
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


            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                menu.findItem(R.id.menu_item_delete).isEnabled = isWorkspaceAdminOrBoardMember
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_item_delete -> {
                        taskDetailsViewModel.deleteTask(
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