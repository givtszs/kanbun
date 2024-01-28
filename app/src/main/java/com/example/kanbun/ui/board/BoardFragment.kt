package com.example.kanbun.ui.board

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.example.kanbun.databinding.FragmentBoardBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.lists_adapter.BoardListsAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
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
//        TODO("Not yet implemented")
    }

    private fun setUpBoardListsAdapter() {
        boardListsAdapter = BoardListsAdapter(
            onCreateListClickListener = {
                buildCreateListDialog()
            },
            onCreateTaskListener = { boardList ->
                buildCreateTaskDialog(boardList)
            },
            navController = navController
        )

        val pagerSnapHelper = PagerSnapHelper()
        binding.rvLists.apply {
            adapter = boardListsAdapter
            pagerSnapHelper.attachToRecyclerView(this)
        }

        val handler = Handler(Looper.getMainLooper())
        var leftWidth = 0
        var rightWidth = 0
        var currentX = 10000 // any value outside the range
        val runnable = object : Runnable {
            override fun run() {
                Log.d(TAG, "currentX: $currentX")

                // Perform scrolling action based on the latest x value
                if (currentX in -1000..(leftWidth - 1000)) {
                    // Scroll to the left
                    binding.rvLists.scrollBy(-2, 0)
                } else if (currentX in 0.. rightWidth) {
                    // Scroll to the right
                    binding.rvLists.scrollBy(2, 0)
                }

                // Post the next scroll after a delay
                handler.postDelayed(this, 1)
            }
        }

        binding.leftSide.setOnDragListener { view, event ->
            val draggableItem = event.localState as View
            leftWidth = view.width

            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    Log.d(TAG, "leftSide: view id - ${view.id}")
                    pagerSnapHelper.attachToRecyclerView(null)
                    handler.postDelayed(runnable, 1)
                    true
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    handler.postDelayed(runnable, 1)
                    currentX = (event.x - 1000).toInt()
                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    currentX = 10000
                    handler.removeCallbacks(runnable)
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    handler.removeCallbacks(runnable)
                    pagerSnapHelper.attachToRecyclerView(binding.rvLists)
                    true
                }

                else -> false
            }
        }

        binding.rightSide.setOnDragListener { view, event ->
            val draggableItem = event.localState as View
            rightWidth = view.width

            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    Log.d(TAG, "rightSide: view id - ${view.id}")
                    pagerSnapHelper.attachToRecyclerView(null)
                    handler.postDelayed(runnable, 1)
                    true
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    handler.postDelayed(runnable, 1)
                    currentX = event.x.toInt()
                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    handler.removeCallbacks(runnable)
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    handler.removeCallbacks(runnable)
                    pagerSnapHelper.attachToRecyclerView(binding.rvLists)
                    true
                }

                else -> false
            }
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
                boardListsAdapter?.setData(lists)
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