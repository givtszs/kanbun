package com.example.kanbun.ui.user_boards

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
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
import com.example.kanbun.ui.buildTextInputDialog
import com.example.kanbun.ui.main_activity.DrawerAdapter
import com.example.kanbun.ui.main_activity.DrawerListeners
import com.example.kanbun.ui.main_activity.MainActivity
import com.google.android.material.appbar.MaterialToolbar
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
        setStatusBarColor(getColor(requireContext(), R.color.background_light))
        setUpBoardsAdapter()
        viewModel.startObservingLifecycle(viewLifecycleOwner.lifecycle)
//        viewModel.init()
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
        setUpDrawer()

        binding.fabCreateBoard.setOnClickListener {
            val workspace = viewModel.userBoardsState.value.workspace
            val user = viewModel.drawerState.value.user
            if (workspace is ViewState.WorkspaceState.WorkspaceReady) {
                buildBoardCreationDialog(user?.id ?: "", workspace.workspace)
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

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.drawerState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        Log.d(TAG, "state: $state")
        when (state) {
            is ViewState.UserBoardsState -> state.process()
            is ViewState.DrawerState -> state.process()
            else -> false
        }
    }

    private fun ViewState.UserBoardsState.process() {
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
                            resources.getString(R.string.empty_shared_boards_tip)
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
            toolbar.title =
                (workspace as? ViewState.WorkspaceState.WorkspaceReady)?.workspace?.name
                    ?: resources.getString(R.string.boards)
            loading.root.isVisible = isLoading
        }
    }

    private fun ViewState.DrawerState.process() {
        if (user == null) {
            return
        }

        activity.userWorkspacesAdapter?.workspaces = user.workspaces.map { workspace ->
            DrawerAdapter.DrawerWorkspace(
                workspace,
                workspace.id == selectedWorkspace
            )
        }.sortedBy { drawerWorkspace ->
            drawerWorkspace.workspace.name
        }

        Log.d(TAG, "processState: sharedWorkspaces: ${user.sharedWorkspaces}")
        activity.sharedWorkspacesAdapter?.workspaces =
            user.sharedWorkspaces.map { workspace ->
                DrawerAdapter.DrawerWorkspace(
                    workspace,
                    workspace.id == selectedWorkspace
                )
            }.sortedBy { drawerWorkspace ->
                drawerWorkspace.workspace.name
            }

        activity.activityMainBinding.drawerContent.headerLayout.apply {
            tvName.text = user.name
            tvTag.text = resources.getString(R.string.user_tag, user.tag)
            tvEmail.text = user.email
            loadProfilePicture(requireContext(), user.profilePicture, ivProfilePicture)
        }
    }

    private fun setUpDrawer() {
        activity.drawerListeners = object : DrawerListeners {
            override fun onSignOutClick() {
                viewModel.signOutUser(requireContext()) {
                    DrawerAdapter.prevSelectedWorkspaceId = null
                    activity.activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
                    navController.navigate(R.id.registrationPromptFragment)
                }
            }

            override fun onSettingsClick() {
                navController.navigate(R.id.action_userBoardsFragment_to_settingsFragment)
            }

            override fun onCreateWorkspaceClick() {
                viewModel.drawerState.value.user?.let { buildWorkspaceCreationDialog(it) }
            }
        }

        DrawerAdapter.onItemClickCallback = { workspaceId ->
            viewModel.selectWorkspace(workspaceId, false)
            activity.activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
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
        buildTextInputDialog(
            context = requireContext(),
            item = R.string.workspace,
            title = R.string.create_workspace
        ) { text ->
            viewModel.createWorkspace(text, user)
            activity.activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun buildBoardCreationDialog(userId: String, workspace: Workspace) {
        buildTextInputDialog(
            context = requireContext(),
            item = R.string.board,
            title = R.string.create_board
        ) { text ->
            Log.d(TAG, "create is clicked: $text, $userId, $workspace")
            viewModel.createBoard(text, userId, workspace)
        }
    }

    private fun setUpBoardsAdapter() {
        boardsAdapter = BoardsAdapter { boardInfo ->
            navController.navigate(
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