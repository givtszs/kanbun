package com.example.kanbun.ui.board_settings

import android.os.Bundle
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
import com.example.kanbun.databinding.FragmentBoardSettingsBinding
import com.example.kanbun.domain.model.Board
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.common_adapters.TagsAdapter
import com.example.kanbun.ui.board_settings.edit_tags.EditTagsBottomSheet
import com.example.kanbun.ui.main_activity.MainActivity
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "BoardSettingsFragment"

class BoardSettingsFragment : BaseFragment(), StateHandler {

    private var _binding: FragmentBoardSettingsBinding? = null
    private val binding: FragmentBoardSettingsBinding get() = _binding!!
    private val args: BoardSettingsFragmentArgs by navArgs()
    private lateinit var board: Board
    private val viewModel: BoardSettingsViewModel by viewModels()
    private var tagsAdapter: TagsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoardSettingsBinding.inflate(inflater, container, false)
        board = args.board
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.toolbar)
        setUpTagsAdapter()
        viewModel.init(board.tags)
        collectState()
    }

    override fun setUpActionBar(toolbar: MaterialToolbar) {
        (requireActivity() as MainActivity).setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            navController.popBackStack()
        }
    }

    override fun setUpListeners() {
        binding.apply {
            etName.setText(board.name)
            etDescription.setText(board.description)

            btnDeleteBoard.setOnClickListener {
                viewModel.deleteBoard(board) {
                    navController.popBackStack(R.id.userBoardsFragment, false)
                }
            }

            btnSave.setOnClickListener {
                // compare only text fields, since tags and members updates are made in place
                val newBoard = board.copy(
                    name = etName.text?.trim().toString(),
                    description = etDescription.text?.trim().toString(),
                    tags = viewModel.boardSettingsState.value.tags.map { it.tag }
                )

                if (newBoard == board) {
                    showToast("No updates")
                    navController.popBackStack()
                } else {
                    viewModel.updateBoard(newBoard) {
                        navController.popBackStack()
                    }
                }
            }

            btnEditTags.setOnClickListener {
                val editTagsDialog = EditTagsBottomSheet.init(
                    tags = viewModel.boardSettingsState.value.tags.map { it.tag }
                )
                editTagsDialog.onDismissCallback = { tags ->
                    viewModel.setTags(tags)
                }
                editTagsDialog.show(childFragmentManager, "edit_tags_dialog")
            }
        }
    }

    override fun collectState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.boardSettingsState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.BoardSettingsViewState) {
            message?.let {
                showToast(it)
                viewModel.messageShown()
            }

            if (tags.isNotEmpty()) {
                tagsAdapter?.tags = tags
            }

            binding.loading.root.isVisible = isLoading
        }
    }

    private fun setUpTagsAdapter() {
        tagsAdapter = TagsAdapter()
        binding.rvTags.adapter = tagsAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        tagsAdapter = null
    }
}