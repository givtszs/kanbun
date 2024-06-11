package com.example.kanbun.ui.board

import android.app.AlertDialog
import android.content.ClipData
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.HORIZONTAL_SCROLL_DISTANCE
import com.example.kanbun.common.TAG
import com.example.kanbun.common.getColor
import com.example.kanbun.common.moshi
import com.example.kanbun.databinding.FragmentBoardBinding
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.TaskList
import com.example.kanbun.domain.model.TaskListInfo
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.task_list.ItemTaskListViewHolder
import com.example.kanbun.ui.board.task_list.TaskListMenuDialog
import com.example.kanbun.ui.board.task_list.TaskListsAdapter
import com.example.kanbun.ui.board.task_list.TaskListsAdapterCallbacks
import com.example.kanbun.ui.buildTextInputDialog
import com.example.kanbun.ui.main_activity.MainActivity
import com.example.kanbun.ui.model.DragAndDropListItem
import com.example.kanbun.ui.model.DragAndDropTaskItem
import com.example.kanbun.ui.shared.SharedBoardViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BoardFragment : BaseFragment(), StateHandler {

    companion object {
        var isBoardMember: Boolean = false
            private set
    }

    private var _binding: FragmentBoardBinding? = null
    private val binding: FragmentBoardBinding get() = _binding!!

    private val boardViewModel: BoardViewModel by viewModels()
    private val sharedViewModel: SharedBoardViewModel by hiltNavGraphViewModels(R.id.board_graph)

    private val args: BoardFragmentArgs by navArgs()

    private var taskListsAdapter: TaskListsAdapter? = null
    private var scrollJob: Job? = null
    private var pagerSnapHelper = PagerSnapHelper()

    private var isBoardFetched = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val boardInfo: Workspace.BoardInfo = args.boardInfo
        getBoard(boardInfo.boardId, boardInfo.workspaceId)
        setUpActionBar(binding.toolbar, boardInfo.name)
        setNavigationBarColor(getColor(requireContext(), R.color.background_light))
        setUpTaskListsAdapter()
        setUpMenu()
        collectState()
    }

    private fun getBoard(boardId: String, workspaceId: String) {
        if (isBoardFetched) return
        boardViewModel.getBoard(boardId, workspaceId) { board ->
            isBoardFetched = true
            isBoardMember = board.members.any { it.id == MainActivity.firebaseUser?.uid }
        }
    }

    override fun setUpListeners() {
        binding.leftSide.setOnDragListener(dragListener(-HORIZONTAL_SCROLL_DISTANCE))
        binding.rightSide.setOnDragListener(dragListener(HORIZONTAL_SCROLL_DISTANCE))
    }

    private val dragListener: (Int) -> View.OnDragListener = { scrollDistance ->
        View.OnDragListener { v, event ->
            val draggableView = event?.localState as View

            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    pagerSnapHelper.attachToRecyclerView(null)
                    true
                }

                DragEvent.ACTION_DRAG_ENTERED -> {
                    scrollJob = lifecycleScope.launch {
                        while (true) {
                            binding.rvLists.scrollBy(scrollDistance, 0)
                            delay(1)
                        }
                    }
                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    scrollJob?.cancel()
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    Log.d("ItemTaskViewHolder", "BoardFragment#ACTION_DRAG_ENDED")
                    scrollJob?.cancel()
                    pagerSnapHelper.attachToRecyclerView(binding.rvLists)
//                    draggableView.visibility = View.VISIBLE
                    true
                }

                else -> false
            }
        }
    }

    private fun setUpTaskListsAdapter() {
        taskListsAdapter = TaskListsAdapter(
            parent = binding.rvLists,
            coroutineScope = lifecycleScope,
            taskDropCallbacks = taskDropCallbacks,
            taskListDropCallback = taskListDropCallback,
            callbacks = object : TaskListsAdapterCallbacks {
                override fun createTaskList() {
                    buildTextInputDialog(
                        context = requireContext(),
                        item = R.string.task_list,
                        title = R.string.create_task_list
                    ) { text ->
                        boardViewModel.createTaskList(text)
                    }
//                    buildCreateListDialog()
                }

                override fun onTaskListMenuClicked(
                    taskList: TaskList,
                    taskLists: List<TaskList>,
                    isEnabled: Boolean
                ) {
                    val taskListMenuDialog = TaskListMenuDialog.init(taskList, taskLists, isEnabled)
                    taskListMenuDialog.show(childFragmentManager, "board_list_menu")
                }

                override fun createTask(taskList: TaskList) {
                    navController.navigate(
                        BoardFragmentDirections.actionBoardFragmentToCreateTaskFragment(
                            taskList = taskList
                        )
                    )
                }

                override fun onTaskClicked(task: Task, taskList: TaskList) {
                    navController.navigate(
                        BoardFragmentDirections.actionBoardFragmentToTaskDetailsFragment(
                            task,
                            taskList = taskList
                        )
                    )
                }
            }
        )

        binding.rvLists.apply {
            adapter = taskListsAdapter
            pagerSnapHelper.attachToRecyclerView(this)
        }
    }

    private val taskDropCallbacks = object : TaskDropCallbacks {
        override fun dropToInsert(
            tasks: List<Task>,
            taskListInfo: TaskListInfo,
            dragItem: DragAndDropTaskItem,
            position: Int
        ) {
            boardViewModel.deleteAndInsert(tasks, taskListInfo, dragItem, position)
        }

        override fun dropToMove(tasks: List<Task>, taskListInfo: TaskListInfo, from: Int, to: Int) {
            if (from != to && to != -1) {
                boardViewModel.rearrangeTasks(
                    listPath = taskListInfo.path,
                    listId = taskListInfo.id,
                    tasks = tasks,
                    from = from,
                    to = to
                )
            }
        }
    }

    private val taskListDropCallback = object : DropCallback {
        override fun <T : RecyclerView.ViewHolder> drop(
            clipData: ClipData,
            adapter: RecyclerView.Adapter<T>,
            position: Int
        ): Boolean {
            val taskListAdapter = adapter as TaskListsAdapter
            val data = clipData.getItemAt(0).text.toString()
            val dragItem = moshi.adapter(DragAndDropListItem::class.java).fromJson(data)

            dragItem?.let {
                boardViewModel.rearrangeLists(
                    taskLists = taskListAdapter.lists,
                    from = dragItem.initPosition,
                    to = ItemTaskListViewHolder.oldPosition
                )
            }

            return true
        }
    }

    private fun setUpMenu() {
        (requireActivity() as MenuHost).addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.board_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.menu_item_settings -> {
                            navController.navigate(
                                BoardFragmentDirections.actionBoardFragmentToBoardSettingsFragment(
                                    boardViewModel.boardState.value.board
                                )
                            )
                            true
                        }

                        else -> false
                    }
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
    }

    override fun collectState() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                boardViewModel.boardState.collect {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.BoardViewState) {
            taskListsAdapter?.lists = lists.sortedBy { it.position }
            Log.d(this@BoardFragment.TAG, "processState: taskLists: ${lists.size}, isLoading: $isLoading")
            binding.rvLists.setItemViewCacheSize(lists.size)
            taskListsAdapter?.boardTags = board.tags
            with(binding.toolbar) {
                if (title != board.name) {
                    title = board.name
                }
            }

            message?.let {
                showToast(it)
                boardViewModel.messageShown()
            }

            sharedViewModel.boardMembers = members
            sharedViewModel.tags = board.tags
            binding.loading.root.isVisible = isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        taskListsAdapter = null
    }

    override fun onDestroy() {
        super.onDestroy()
        isBoardMember = false
    }
}