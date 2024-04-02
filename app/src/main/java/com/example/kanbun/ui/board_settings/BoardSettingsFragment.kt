package com.example.kanbun.ui.board_settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
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
import com.example.kanbun.ui.manage_members.SearchUsersAdapter
import com.example.kanbun.ui.manage_members.MembersAdapter
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Job
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
    private var searchUsersAdapter: SearchUsersAdapter? = null
    private var boardMembersAdapter: MembersAdapter? = null

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
        setUpAdapters()
        viewModel.init(board.tags, board.members, board.workspace.id)
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

            var searchJob: Job? = null
            etSearchUser.setOnFocusChangeListener { view, isFocused ->
                if (isFocused && etSearchUser.text?.isEmpty() == true) {
                    viewModel.resetFoundUsers()
                } else if (!isFocused) {
                    viewModel.resetFoundUsers(true)
                }
            }
            etSearchUser.doOnTextChanged { text, _, _, _ ->
                searchJob?.cancel()
                if (!text.isNullOrEmpty() && text.length >= 3) {
                    Log.d(TAG, "searchUsers: $text")
                    searchJob = viewModel.searchUser(text.toString())
                } else {
                    Log.d(TAG, "searchUser: call resetFoundUsers")
                    viewModel.resetFoundUsers()
                }
            }

            btnDeleteBoard.setOnClickListener {
                viewModel.deleteBoard(board) {
                    navController.popBackStack(R.id.userBoardsFragment, false)
                }
            }

            btnSave.setOnClickListener {
                // compare only text fields, since tags and members updates are made in place
                val newBoard = board.copy(
                    name = etName.text?.trim().toString().ifEmpty {
                        tfName.apply {
                            error = "Workspace name can't be empty"
                            isErrorEnabled = true
                        }
                        return@setOnClickListener
                    },
                    description = etDescription.text?.trim().toString(),
                    tags = viewModel.boardSettingsState.value.tags.map { it.tag },
                    members = viewModel.boardMembers.value
                )

                if (newBoard == board) {
                    showToast("No updates")
                    navController.popBackStack()
                } else {
                    viewModel.updateBoard(board, newBoard) {
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

    private fun setUpAdapters() {
        searchUsersAdapter = SearchUsersAdapter() { user ->
            showToast("Clicked on ${user.tag}")
            if (user.id != board.owner) {
                viewModel.addMember(user)
            }
        }
        binding.rvFoundUsers.adapter = searchUsersAdapter

        boardMembersAdapter = MembersAdapter(ownerId = board.owner) { member ->
            viewModel.removeMember(member)
        }
        binding.rvMembers.adapter = boardMembersAdapter
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
            binding.apply {
                tagsAdapter?.tags = tags
                loading.root.isVisible = isLoading
                message?.let {
                    showToast(it)
                    viewModel.messageShown()
                }

                rvFoundUsers.isVisible = foundUsers != null
                foundUsers?.let { users ->
                    // TODO: DON'T FORGET TO UPDATE ME!!!
//                    searchUsersAdapter?.users = users
                }

                Log.d(TAG, "processState: members: $boardMembers")
//                searchUsersAdapter?.members = boardMembers.map { it.id }
                boardMembersAdapter?.members = boardMembers
            }
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
        searchUsersAdapter = null
        boardMembersAdapter = null
    }
}