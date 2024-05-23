package com.example.kanbun.ui.board_settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.kanbun.R
import com.example.kanbun.common.Role
import com.example.kanbun.databinding.FragmentBoardSettingsBinding
import com.example.kanbun.domain.model.Board
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.BoardFragment
import com.example.kanbun.ui.board.common_adapters.TagsAdapter
import com.example.kanbun.ui.board_settings.edit_tags.EditTagsBottomSheet
import com.example.kanbun.ui.buildDeleteConfirmationDialog
import com.example.kanbun.ui.main_activity.MainActivity
import com.example.kanbun.ui.manage_members.MembersAdapter
import com.example.kanbun.ui.manage_members.MembersBottomSheet
import com.example.kanbun.ui.manage_members.SearchUsersAdapter
import com.example.kanbun.ui.shared.SharedBoardViewModel
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "BoardSettingsFragment"

class BoardSettingsFragment : BaseFragment(), StateHandler {

    private var _binding: FragmentBoardSettingsBinding? = null
    private val binding: FragmentBoardSettingsBinding get() = _binding!!

    private val args: BoardSettingsFragmentArgs by navArgs()
    private val board: Board by lazy {
        args.board
    }
    private val isUserAdmin: Boolean by lazy {
        board.members
            .find { it.id == MainActivity.firebaseUser?.uid }
            ?.role == Role.Board.Admin
    }

    private val boardSettingsViewModel: BoardSettingsViewModel by viewModels()
    private val sharedViewModel: SharedBoardViewModel by hiltNavGraphViewModels(R.id.board_graph)

    private var tagsAdapter: TagsAdapter? = null
    private var searchUsersAdapter: SearchUsersAdapter? = null
    private var boardMembersAdapter: MembersAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoardSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.toolbar)
        setUpTagsAdapter()
        setUpSearchUsersAdapter()
        setUpMembersAdapter()
        boardSettingsViewModel.init(
            board.tags,
            board.owner,
            sharedViewModel.boardMembers,
            board.members,
            board.workspace.id
        )
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
            tfName.isEnabled = isUserAdmin
            etName.setText(board.name)

            tfDescription.isEnabled = isUserAdmin
            etDescription.setText(board.description)

            tfSearchUser.isEnabled = isUserAdmin
            var searchJob: Job? = null
            etSearchUser.setOnFocusChangeListener { _, isFocused ->
                boardSettingsViewModel.resetFoundUsers(!isFocused)
            }
            etSearchUser.doOnTextChanged { text, _, _, _ ->
                searchJob?.cancel()
                if (!text.isNullOrEmpty() && text.length >= 3) {
                    searchJob = boardSettingsViewModel.searchUser(text.toString())
                } else {
                    boardSettingsViewModel.resetFoundUsers()
                }
            }
            etSearchUser.setOnClickListener {
                if (etSearchUser.isFocused) {
                    etSearchUser.clearFocus()
                    rvFoundUsers.isVisible = false
                    hideKeyboard(etSearchUser)
                }
            }

            btnDeleteBoard.isEnabled = isUserAdmin
            btnDeleteBoard.setOnClickListener {
                buildDeleteConfirmationDialog(
                    requireContext(),
                    R.string.delete_board_dialog_title
                ) {
                    boardSettingsViewModel.deleteBoard(board) {
                        navController.popBackStack(R.id.userBoardsFragment, false)
                    }
                }.show()
            }

            btnLeaveBoard.isVisible =
                BoardFragment.isBoardMember && MainActivity.firebaseUser?.uid != board.owner
            btnLeaveBoard.setOnClickListener {
                boardSettingsViewModel.leaveBoard(board, MainActivity.firebaseUser?.uid) {
                    navController.navigate(R.id.action_to_userBoardsFragment)
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
                    tags = boardSettingsViewModel.boardSettingsState.value.tags.map { it.tag },
                    members = boardSettingsViewModel.boardSettingsState.value.boardMembers.map { member ->
                        Log.d(TAG, "cast members: $member")
                        Board.BoardMember(id = member.user.id, role = member.role as Role.Board)
                    }
                )

                if (newBoard == board) {
                    showToast("No updates")
                    navController.popBackStack()
                } else {
                    boardSettingsViewModel.updateBoard(board, newBoard) {
                        navController.popBackStack()
                    }
                }
            }

            btnEditTags.setOnClickListener {
                val editTagsDialog = EditTagsBottomSheet.init(
                    tags = boardSettingsViewModel.boardSettingsState.value.tags.map { it.tag },
                    isEditable = board.members.any { it.id == MainActivity.firebaseUser?.uid }
                )
                editTagsDialog.onDismissCallback = { tags ->
                    boardSettingsViewModel.setTags(tags)
                }
                editTagsDialog.show(childFragmentManager, "edit_tags_dialog")
            }

            btnViewAllMembers.setOnClickListener {
                val membersBottomSheet =
                    MembersBottomSheet.init(
                        members = boardSettingsViewModel.boardSettingsState.value.boardMembers,
                        ownerId = board.owner
                    ) { members ->
                        boardSettingsViewModel.setMembers(members)
                    }
                membersBottomSheet.show(childFragmentManager, "board_members")
            }
        }
    }

    private fun setUpTagsAdapter() {
        tagsAdapter = TagsAdapter()
        binding.rvTags.adapter = tagsAdapter
    }

    private fun setUpSearchUsersAdapter() {
        searchUsersAdapter = SearchUsersAdapter() { user ->
            showToast("Clicked on ${user.tag}")
            if (user.id != board.owner) {
                boardSettingsViewModel.addMember(user)
            }
        }
        binding.rvFoundUsers.adapter = searchUsersAdapter


    }

    private fun setUpMembersAdapter() {
        boardMembersAdapter = MembersAdapter(ownerId = board.owner) { member ->
            boardSettingsViewModel.removeMember(member.user)
        }
        binding.rvMembers.adapter = boardMembersAdapter
    }

    override fun collectState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                boardSettingsViewModel.boardSettingsState.collectLatest {
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
                    boardSettingsViewModel.messageShown()
                }

                rvFoundUsers.isVisible = foundUsers != null
                foundUsers?.let { users ->
                    searchUsersAdapter?.users = users.map { user ->
                        user.copy(
                            isAdded = boardMembers.any { it.user.id == user.user.id }
                        )
                    }
                }

                boardMembersAdapter?.members = boardMembers
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        tagsAdapter = null
        searchUsersAdapter = null
        boardMembersAdapter = null
    }
}