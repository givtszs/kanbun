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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.HORIZONTAL_SCROLL_DISTANCE
import com.example.kanbun.common.Role
import com.example.kanbun.common.TaskAction
import com.example.kanbun.common.moshi
import com.example.kanbun.databinding.FragmentBoardBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.board_list.BoardListMenuDialog
import com.example.kanbun.ui.board.board_list.BoardListsAdapter
import com.example.kanbun.ui.board.board_list.BoardListsAdapterCallbacks
import com.example.kanbun.ui.board.board_list.ItemBoardListViewHolder
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.main_activity.MainActivity
import com.example.kanbun.ui.model.DragAndDropListItem
import com.example.kanbun.ui.model.DragAndDropTaskItem
import com.example.kanbun.ui.shared.SharedViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "BoardFragment"

@AndroidEntryPoint
class BoardFragment : BaseFragment(), StateHandler {

    private var _binding: FragmentBoardBinding? = null
    private val binding: FragmentBoardBinding get() = _binding!!

    private val boardViewModel: BoardViewModel by viewModels()
    private val membersViewModel: SharedViewModel by activityViewModels()

    private val args: BoardFragmentArgs by navArgs()

    private var boardListsAdapter: BoardListsAdapter? = null
    private var scrollJob: Job? = null
    private var pagerSnapHelper = PagerSnapHelper()

    private var areBoardMembersFetched = false
    private var isWorkspaceAdminOrBoardMember = false

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
        val boardInfo = args.boardInfo
        setUpActionBar(binding.topAppBar.toolbar, boardInfo.name)
        setUpBoardListsAdapter()
        setUpMenu()
        collectState()
        boardViewModel.getBoard(boardInfo.boardId, boardInfo.workspaceId)
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

    private fun setUpBoardListsAdapter() {
        boardListsAdapter = BoardListsAdapter(
            coroutineScope = lifecycleScope,
            taskDropCallbacks = taskDropCallbacks,
            boardListDropCallback = boardListDropCallback,
            callbacks = object : BoardListsAdapterCallbacks {
                override fun createBoardList() {
                    buildCreateListDialog()
                }

                override fun onBoardListMenuClicked(boardList: BoardList, boardLists: List<BoardList>, isEnabled: Boolean) {
                    val boardListMenuDialog = BoardListMenuDialog.init(boardList, boardLists, isEnabled)
                    boardListMenuDialog.show(childFragmentManager, "board_list_menu")
                }

                override fun createTask(boardList: BoardList) {
                    navController.navigate(
                        BoardFragmentDirections.actionBoardFragmentToCreateTaskFragment(
                            actionType = TaskAction.ACTION_CREATE,
                            task = Task(
                                position = boardList.tasks.size.toLong()
                            ),
                            boardList = boardList
                        )
                    )
                }

                override fun onTaskClicked(task: Task, boardList: BoardList) {
                    navController.navigate(
                        BoardFragmentDirections.actionBoardFragmentToTaskDetailsFragment(
                            task,
                            boardList = boardList,
                            isWorkspaceAdminOrBoardMember = this@BoardFragment.isWorkspaceAdminOrBoardMember
                        )
                    )
                }

                override fun loadingComplete() {
//                    viewModel.stopLoading()
                }
            }
        ).apply {
            isWorkspaceAdminOrBoardMember = this@BoardFragment.isWorkspaceAdminOrBoardMember
        }

        binding.rvLists.apply {
            adapter = boardListsAdapter
            pagerSnapHelper.attachToRecyclerView(this)
        }
    }

    private val taskDropCallbacks = object : TaskDropCallbacks {
        override fun dropToInsert(
            adapter: TasksAdapter,
            dragItem: DragAndDropTaskItem,
            position: Int
        ) {
            boardViewModel.deleteAndInsert(adapter, dragItem, position)
        }

        override fun dropToMove(adapter: TasksAdapter, from: Int, to: Int) {
            if (from != to && to != -1) {
                boardViewModel.rearrangeTasks(
                    listPath = adapter.listInfo.path,
                    listId = adapter.listInfo.id,
                    tasks = adapter.tasks,
                    from = from,
                    to = to
                )
            }
        }
    }

    private val boardListDropCallback = object : DropCallback {
        override fun <T : RecyclerView.ViewHolder> drop(
            clipData: ClipData,
            adapter: RecyclerView.Adapter<T>,
            position: Int
        ): Boolean {
            val boardListAdapter = adapter as BoardListsAdapter
            val data = clipData.getItemAt(0).text.toString()
            val dragItem = moshi.adapter(DragAndDropListItem::class.java).fromJson(data)

            dragItem?.let {
                boardViewModel.rearrangeLists(
                    boardLists = boardListAdapter.lists,
                    from = dragItem.initPosition,
                    to = ItemBoardListViewHolder.oldPosition
                )
            }

            return true
        }
    }

    private fun buildCreateListDialog() {
        buildAlertDialog(
            dialogTitle = "Create list",
            editTextHint = "Enter a new list name",
            createCallback = { name ->
                boardViewModel.createBoardList(name)
            }
        )
    }

    private fun buildCreateTaskDialog(boardList: BoardList) {
        buildAlertDialog(
            dialogTitle = "Create task",
            editTextHint = "Enter a new task name",
            createCallback = { name ->
                boardViewModel.createTask(name, boardList)
            }
        )
    }

    private fun buildAlertDialog(
        dialogTitle: String,
        editTextHint: String,
        createCallback: (String) -> Unit
    ) {
        val editText = EditText(requireContext()).apply {
            hint = editTextHint
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                createCallback(editText.text.trim().toString())
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .apply {
                setOnShowListener {
                    val posButton = this.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                        isEnabled = false
                    }

                    editText.doOnTextChanged { text, _, _, _ ->
                        posButton.isEnabled = text?.trim().isNullOrEmpty() == false
                    }
                }
            }
            .show()
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
            // TODO("Change Lifecycle.State to RESUMED if somethings is wrong")
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                boardViewModel.boardState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.BoardViewState) {
            boardListsAdapter?.setData(lists.sortedBy { it.position })
            boardListsAdapter?.boardTags = board.tags
            isWorkspaceAdminOrBoardMember = args.userWorkspaceRole == Role.Workspace.Admin || board.members.any { it.id == MainActivity.firebaseUser?.uid }


            Log.d(TAG, "processState: isLoading: $isLoading")
            binding.loading.root.isVisible = isLoading

            message?.let {
                showToast(it)
                boardViewModel.messageShown()
            }

            if (board.id.isNotEmpty() && !areBoardMembersFetched) {
                membersViewModel.getBoardMembers(board.members.map { it.id })
                areBoardMembersFetched = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        boardListsAdapter = null
    }
}