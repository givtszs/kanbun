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
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kanbun.R
import com.example.kanbun.common.DrawerItem
import com.example.kanbun.common.RECYCLERVIEW_BOARDS_COLUMNS
import com.example.kanbun.common.Role
import com.example.kanbun.common.TAG
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
        var workspaceRole: Role.Workspace? = null
            private set
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
                        "onMenuItemSelected: workspace_settings: workspace: ${viewModel.userBoardsState.value.workspace}"
                    )

                    (viewModel.userBoardsState.value.workspace as? ViewState.WorkspaceState.WorkspaceReady)?.let { workspace ->
                        navController.navigate(
                            UserBoardsFragmentDirections
                                .actionUserBoardsFragmentToWorkspaceSettingsFragment(workspace.workspace)
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
        addOnBackPressedAction { requireActivity().finish() }
        setUpActionBar(binding.toolbar)
        setStatusBarColor(getColor(requireContext(), R.color.background_secondary))
        setUpBoardsAdapter()
        viewModel.init()
        viewModel.checkUserVerification(
            nullUserCallback = {
                showToast("Firebase user is null")
                navController.navigate(R.id.action_userBoardsFragment_to_registrationPromptFragment)
            },
            failedVerificationCallback = { provider ->
                showToast("Complete registration by signing in with $provider and verifying your email")
                navController.navigate(UserBoardsFragmentDirections.actionUserBoardsFragmentToRegistrationPromptFragment())
            }
        )
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
                (workspace as? ViewState.WorkspaceState.WorkspaceReady)?.let { workspace ->
                    buildBoardCreationDialog(user?.id ?: "", workspace.workspace)
                }
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
        with(state as ViewState.UserBoardsState) {
            setUserState()

            when (workspace) {
                ViewState.WorkspaceState.NullWorkspace -> {
                    removeOptionsMenu()
                    boardsAdapter?.clear()
                    binding.tvTip.text = resources.getString(R.string.workspace_selection_tip)
                    binding.ivContextImage.setImageResource(R.drawable.undraw_select_option_menu)
                }

                is ViewState.WorkspaceState.WorkspaceReady -> {
                    // set options menu
                    if (!isMenuProviderAdded && workspace.workspace.id != DrawerItem.SHARED_BOARDS) {
                        createOptionsMenu()
                        isMenuProviderAdded = true
                    } else if (workspace.workspace.id == DrawerItem.SHARED_BOARDS) {
                        removeOptionsMenu()
                    }

                    if (workspace.workspace.boards.isEmpty()) {
                        binding.tvTip.text =
                            if (workspace.workspace.id != DrawerItem.SHARED_BOARDS) {
                                resources.getString(R.string.empty_workspace_tip)
                            } else {
                                "Shared boards will appear here"
                            }
                        binding.ivContextImage.setImageResource(R.drawable.undraw_board)
                    }

                    workspaceRole = workspace.workspace.members[MainActivity.firebaseUser?.uid]
                    Log.d(this@UserBoardsFragment.TAG, "boards: ${workspace.workspace.boards}")
                    boardsAdapter?.setData(workspace.workspace.boards)
                    DrawerAdapter.prevSelectedWorkspaceId = workspace.workspace.id
                }
            }

            message?.let {
                showToast(it)
                viewModel.messageShown()
            }

            binding.apply {
                activity.isSharedBoardsSelected =
                    (workspace as? ViewState.WorkspaceState.WorkspaceReady)?.workspace?.id == DrawerItem.SHARED_BOARDS
                tvTip.isVisible = workspace is ViewState.WorkspaceState.NullWorkspace ||
                        (workspace as ViewState.WorkspaceState.WorkspaceReady).workspace.boards.isEmpty()
                ivContextImage.isVisible = tvTip.isVisible
                rvBoards.isVisible = workspace !is ViewState.WorkspaceState.NullWorkspace
                fabCreateBoard.isVisible = workspace is ViewState.WorkspaceState.WorkspaceReady &&
                        workspace.workspace.id != DrawerItem.SHARED_BOARDS &&
                        workspaceRole == Role.Workspace.Admin
                toolbar.title = (workspace as? ViewState.WorkspaceState.WorkspaceReady)?.workspace?.name
                    ?: resources.getString(R.string.boards)
                loading.root.isVisible = isLoading
            }
        }
    }

    private fun ViewState.UserBoardsState.setUserState() {
        if (user == null) {
            return
        }

        setUpDrawer(user)
        activity.userWorkspacesAdapter?.workspaces = user.workspaces.map { workspace ->
            DrawerAdapter.DrawerWorkspace(
                workspace,
                workspace.id == (this.workspace as? ViewState.WorkspaceState.WorkspaceReady)?.workspace?.id
            )
        }.sortedBy { drawerWorkspace ->
            drawerWorkspace.workspace.name
        }

        Log.d(TAG, "processState: sharedWorkspaces: ${user.sharedWorkspaces}")
        activity.sharedWorkspacesAdapter?.workspaces =
            user.sharedWorkspaces.map { workspace ->
                DrawerAdapter.DrawerWorkspace(
                    workspace,
                    workspace.id == (this.workspace as? ViewState.WorkspaceState.WorkspaceReady)?.workspace?.id
                )
            }.sortedBy { drawerWorkspace ->
                drawerWorkspace.workspace.name
            }
    }

    private fun setUpDrawer(user: User) {
        with(activity) {
            // set up header layout

            activityMainBinding.drawerLayout.addDrawerListener(object : DrawerListener {
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
//                    TODO("Not yet implemented")
                    setStatusBarColor(getColor(requireContext(), R.color.background_primary))
                }

                override fun onDrawerOpened(drawerView: View) {
//                    TODO("Not yet implemented")

                }

                override fun onDrawerClosed(drawerView: View) {
//                    TODO("Not yet implemented")
                    setStatusBarColor(getColor(requireContext(), R.color.background_secondary))
                }

                override fun onDrawerStateChanged(newState: Int) {
//                    TODO("Not yet implemented")
                }
            })
            activityMainBinding.drawerContent.headerLayout.apply {
                tvName.text = user.name
                tvTag.text = user.tag
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
                Log.d(TAG, "create is clicked: ${editText.text.toString()}, $userId, $workspace")
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