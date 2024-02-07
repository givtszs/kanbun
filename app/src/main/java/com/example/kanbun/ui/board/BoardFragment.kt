package com.example.kanbun.ui.board

import android.app.AlertDialog
import android.content.ClipData
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.FirestoreCollection
import com.example.kanbun.common.HORIZONTAL_SCROLL_DISTANCE
import com.example.kanbun.common.TaskAction
import com.example.kanbun.databinding.FragmentBoardBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Task
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.lists_adapter.BoardListsAdapter
import com.example.kanbun.ui.board.lists_adapter.ItemBoardListViewHolder
import com.example.kanbun.ui.board.tasks_adapter.TasksAdapter
import com.example.kanbun.ui.model.DragAndDropListItem
import com.example.kanbun.ui.model.DragAndDropTaskItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.moshi.Moshi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "BoardFragm"

@AndroidEntryPoint
class BoardFragment : BaseFragment(), StateHandler {
    private var _binding: FragmentBoardBinding? = null
    private val binding: FragmentBoardBinding get() = _binding!!
    private val viewModel: BoardViewModel by viewModels()
    private val args: BoardFragmentArgs by navArgs()
    private lateinit var boardInfo: Workspace.BoardInfo
    private var boardListsAdapter: BoardListsAdapter? = null

    private var scrollJob: Job? = null
    private var pagerSnapHelper = PagerSnapHelper()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoardBinding.inflate(inflater, container, false)
        boardInfo = args.boardInfo
        Log.d(TAG, "boardInfo: $boardInfo")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.topAppBar.toolbar, boardInfo.name)
        setUpBoardListsAdapter()
        collectState()
        lifecycleScope.launch {
            viewModel.getBoard(boardInfo.boardId, boardInfo.workspaceId)
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

    private fun setUpBoardListsAdapter() {
        boardListsAdapter = BoardListsAdapter(
            coroutineScope = lifecycleScope,
            taskDropCallback = taskDropCallbacks,
            boardListDropCallback = boardListDropCallback,
            onCreateListClickListener = {
                buildCreateListDialog()
            },
            onCreateTaskListener = { boardList ->
//                buildCreateTaskDialog(boardList)
                navController.navigate(
                    BoardFragmentDirections.actionBoardFragmentToCreateTaskFragment(
                        actionType = TaskAction.ACTION_CREATE,
                        task = Task(
                            boardListInfo = BoardListInfo(boardList.id, boardList.path),
                            position = boardList.tasks.size.toLong()
                        )
                    )
                )
            },
            loadingCompleteCallback = { viewModel.stopLoading() },
            onTaskClicked = { task ->
                navController.navigate(
                    BoardFragmentDirections.actionBoardFragmentToTaskDetailsFragment(
                        task
                    )
                )
            }
        )

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
            viewModel.deleteAndInsert(adapter, dragItem, position)
        }

        override fun dropToMove(adapter: TasksAdapter, from: Int, to: Int) {
            if (from != to && to != -1) {
                viewModel.rearrangeTasks(
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
            val moshi = Moshi.Builder().build()
            val jsonAdapter = moshi.adapter(DragAndDropListItem::class.java)
            val dragItem = jsonAdapter.fromJson(data)

            dragItem?.let {
                viewModel.rearrangeLists(
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
                viewModel.createBoardList(name)
            }
        )
    }

    private fun buildCreateTaskDialog(boardList: BoardList) {
        buildAlertDialog(
            dialogTitle = "Create task",
            editTextHint = "Enter a new task name",
            createCallback = { name ->
                viewModel.createTask(name, boardList)
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

    override fun collectState() {
        lifecycleScope.launch {
            // TODO("Change Lifecycle.State to RESUMED if somethings is wrong")
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.boardState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.BoardViewState) {
            if (lists.isNotEmpty()) {
                boardListsAdapter?.setData(lists.sortedBy { it.position })
//                viewModel.stopLoading()
            } else {
                viewModel.stopLoading()
            }

            binding.loading.root.isVisible = isLoading

            message?.let {
                showToast(it)
                viewModel.messageShown()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        boardListsAdapter = null
    }
}