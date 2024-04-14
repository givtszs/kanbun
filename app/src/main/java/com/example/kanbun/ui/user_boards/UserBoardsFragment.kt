package com.example.kanbun.ui.user_boards

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.GravityCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.kanbun.R
import com.example.kanbun.common.DrawerItem
import com.example.kanbun.common.RECYCLERVIEW_BOARDS_COLUMNS
import com.example.kanbun.common.Role
import com.example.kanbun.common.getColor
import com.example.kanbun.common.loadProfilePicture
import com.example.kanbun.databinding.FragmentUserBoardsBinding
import com.example.kanbun.domain.model.User
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.main_activity.DrawerAdapter
import com.example.kanbun.ui.main_activity.MainActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "UserBoardsFragm"

@AndroidEntryPoint
class UserBoardsFragment : BaseFragment(), StateHandler {

    companion object {
        private var _workspaceRole: Role.Workspace? = null
        val userRole: Role.Workspace? get() = _workspaceRole
    }

    private var _binding: FragmentUserBoardsBinding? = null
    private val binding: FragmentUserBoardsBinding get() = _binding!!

    private val viewModel: UserBoardsViewModel by viewModels()
    private lateinit var activity: MainActivity

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            if (menu.isEmpty()) {
                menuInflater.inflate(R.menu.workspace_menu, menu)
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_item_settings -> {
                    Log.d(
                        TAG,
                        "onMenuItemSelected: workspace_settings: workspace: ${viewModel.userBoardsState.value.currentWorkspace}"
                    )

                    viewModel.userBoardsState.value.currentWorkspace?.let { workspace ->
                        navController.navigate(
                            UserBoardsFragmentDirections
                                .actionUserBoardsFragmentToWorkspaceSettingsFragment(workspace)
                        )
                    }

                    true
                }

                else -> false
            }
        }
    }

    private var isMenuProviderAdded = false
    private var boardsAdapter: BoardsAdapter? = null
    private var isInitCalled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBoardsBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.topAppBar.toolbar)
        setStatusBarColor(getColor(requireContext(), R.color.md_theme_light_surface))
        addOnBackPressedAction { requireActivity().finish() }
        if (!isInitCalled) {
            viewModel.init(navController)
            isInitCalled = true
        }
        setUpBoardsAdapter()
        collectState()
    }

    override fun setUpListeners() {
        // set up drawers listeners
        activity.apply {
            activityMainBinding.drawerContent.headerLayout.apply {
                btnSignOut.setOnClickListener {
                    viewModel.signOutUser(requireContext()) {
                        DrawerAdapter.prevSelectedWorkspaceId = null
                        navController.navigate(R.id.registrationPromptFragment)
                    }
                }

                btnSettings.setOnClickListener {
                    navController.navigate(R.id.action_userBoardsFragment_to_settingsFragment)
                }
            }

            DrawerAdapter.onItemClickCallback = { workspaceId ->
                viewModel.selectWorkspace(workspaceId, false)
                activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

        binding.fabCreateBoard.setOnClickListener {
            with(viewModel.userBoardsState.value) {
                buildBoardCreationDialog(user?.id ?: "", currentWorkspace ?: Workspace())
            }
        }
    }

    override fun setUpActionBar(toolbar: MaterialToolbar) {
        activity.apply {
            setSupportActionBar(toolbar)
            setupActionBarWithNavController(navController, appBarConfiguration)
        }
    }

    override fun collectState() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userBoardsState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.UserBoardsViewState) {
//            Log.d(TAG, "State: $this")
            user?.let { _user ->
                setUpDrawer(_user)
                Log.d(TAG, "processState: userWorkspaces: ${_user.workspaces}")
                activity.userWorkspacesAdapter?.workspaces = _user.workspaces.map { workspace ->
                    DrawerAdapter.DrawerWorkspace(
                        workspace,
                        workspace.id == currentWorkspace?.id
                    )
                }.sortedBy { drawerWorkspace ->
                    drawerWorkspace.workspace.name
                }

                Log.d(TAG, "processState: sharedWorkspaces: ${_user.sharedWorkspaces}")
                activity.sharedWorkspacesAdapter?.workspaces =
                    _user.sharedWorkspaces.map { workspace ->
                        DrawerAdapter.DrawerWorkspace(
                            workspace,
                            workspace.id == currentWorkspace?.id
                        )
                    }.sortedBy { drawerWorkspace ->
                        drawerWorkspace.workspace.name
                    }
            }

            binding.apply {
                loading.root.isVisible = isLoading
                rvBoards.isVisible = currentWorkspace != null
                fabCreateBoard.isVisible =
                    currentWorkspace != null && currentWorkspace.id != DrawerItem.SHARED_BOARDS && userRole == Role.Workspace.Admin
                activity.isSharedBoardsSelected = currentWorkspace?.id == DrawerItem.SHARED_BOARDS
                tvTip.isVisible = currentWorkspace == null || currentWorkspace.boards.isEmpty()
                topAppBar.toolbar.title =
                    currentWorkspace?.name ?: resources.getString(R.string.boards)

                // create options menu
                if (!isMenuProviderAdded && currentWorkspace != null && currentWorkspace.id != DrawerItem.SHARED_BOARDS) {
                    createOptionsMenu()
                    isMenuProviderAdded = true
                } else if (currentWorkspace == null || currentWorkspace.id == DrawerItem.SHARED_BOARDS) {
                    removeOptionsMenu()
                }

                if (currentWorkspace != null) {
                    Log.d(TAG, "currentWorkspace: $currentWorkspace")
                    _workspaceRole = currentWorkspace.members[MainActivity.firebaseUser?.uid]
                    boardsAdapter?.setData(currentWorkspace.boards)
                    DrawerAdapter.prevSelectedWorkspaceId = currentWorkspace.id

                    if (currentWorkspace.boards.isEmpty()) {
                        tvTip.text = if (currentWorkspace.id != DrawerItem.SHARED_BOARDS) {
                            resources.getString(R.string.empty_workspace_tip)
                        } else {
                            "Shared boards will appear here"
                        }
                    }
                } else {
                    boardsAdapter?.clear()
                    tvTip.text = resources.getString(R.string.workspace_selection_tip)
                }
            }

            message?.let {
                showToast(it)
                viewModel.messageShown()
            }
        }
    }

    private fun setUpDrawer(user: User) {
        with(activity) {
            // set up header layout
            activityMainBinding.drawerContent.headerLayout.apply {
                tvName.text = user.name
                tvEmail.text = user.email
                loadProfilePicture(requireContext(), user.profilePicture, ivProfilePicture)
            }

            activityMainBinding.drawerContent.btnCreateWorkspace.setOnClickListener {
                buildWorkspaceCreationDialog(user)
            }
        }
    }

    private fun createOptionsMenu() {
        activity.addMenuProvider(
            menuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun removeOptionsMenu() {
        activity.removeMenuProvider(menuProvider)
        isMenuProviderAdded = false
    }

    private fun buildWorkspaceCreationDialog(user: User) {
        val editText = EditText(requireContext()).apply {
            hint = "Enter a new workspace name"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create workspace")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                viewModel.createWorkspace(editText.text.toString(), user)
                activity.activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }
            .create().apply {
                setOnShowListener {
                    val posButton =
                        getButton(AlertDialog.BUTTON_POSITIVE).apply { isEnabled = false }

                    editText.doOnTextChanged { text, _, _, _ ->
                        posButton.isEnabled = text?.trim().isNullOrEmpty() == false
                    }
                }
            }
            .show()
    }

    private fun buildBoardCreationDialog(userId: String, workspace: Workspace) {
        val editText = EditText(requireContext()).apply {
            hint = "Enter a new board name"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create board")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                viewModel.createBoard(editText.text.toString(), userId, workspace)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .apply {
                setOnShowListener {
                    val posButton =
                        getButton(AlertDialog.BUTTON_POSITIVE).apply { isEnabled = false }

                    editText.doOnTextChanged { text, _, _, _ ->
                        posButton.isEnabled = text?.trim().isNullOrEmpty() == false
                    }
                }
            }
            .show()
    }

    private fun setUpBoardsAdapter() {
        boardsAdapter = BoardsAdapter { boardInfo ->
            navController.navigate(
//                UserBoardsFragmentDirections.actionUserBoardsFragmentToBoardFragment(boardInfo = boardInfo)
                UserBoardsFragmentDirections.actionUserBoardsFragmentToBoardGraph(boardInfo = boardInfo)
            )
        }

        binding.rvBoards.apply {
            adapter = boardsAdapter
            layoutManager = GridLayoutManager(requireContext(), RECYCLERVIEW_BOARDS_COLUMNS)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeOptionsMenu()
        boardsAdapter = null
        _binding = null
    }
}