package com.example.kanbun.ui.board

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.kanbun.databinding.FragmentBoardBinding
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.adapter.BoardListsAdapter
import dagger.hilt.android.AndroidEntryPoint
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
        viewModel.initBoard(boardInfo.boardId, boardInfo.workspaceId)
    }

    override fun setUpListeners() {
//        TODO("Not yet implemented")
    }

    private fun setUpBoardListsAdapter() {
        boardListsAdapter = BoardListsAdapter(
            onCreateListClickListener = {
                showToast("Create list is clicked")
            }
        )
        binding.rvLists.apply {
            adapter = boardListsAdapter
            PagerSnapHelper().attachToRecyclerView(this)
        }

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