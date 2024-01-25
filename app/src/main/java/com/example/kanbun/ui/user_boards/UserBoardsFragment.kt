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
import com.example.kanbun.common.AuthProvider
import com.example.kanbun.common.RECYCLERVIEW_BOARDS_COLUMNS
import com.example.kanbun.common.getColor
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
                R.id.workspace_settings -> {
                    Log.d(
                        TAG,
                        "onMenuItemSelected: workspace_settings: workspace: ${viewModel.userBoardsState.value.currentWorkspace}"
                    )
                    navController.navigate(
                        UserBoardsFragmentDirections.actionUserBoardsFragmentToWorkspaceSettingsFragment(
                            viewModel.userBoardsState.value.currentWorkspace ?: Workspace()
                        )
                    )
                    true
                }

                else -> false
            }
        }
    }

    private var boardsAdapter: BoardsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBoardsBinding.inflate(inflater, container, false)
        viewModel.getCurrentWorkspace()
        activity = requireActivity() as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.topAppBar.toolbar)
        setStatusBarColor(getColor(requireContext(), R.color.md_theme_light_surface))
        addOnBackPressedAction { requireActivity().finish() }
        checkUserAuthState()
        setUpBoardsAdapter()
        collectState()
    }

    override fun setUpListeners() {
        // set up drawers listeners
        activity.apply {
            activityMainBinding.headerLayout.btnSignOut.setOnClickListener {
                lifecycleScope.launch {
                    viewModel.signOutUser(requireContext())
                    navController.navigate(R.id.registrationPromptFragment)
                }
            }

            drawerAdapter?.onItemClickCallback = { workspaceId ->
                viewModel.selectWorkspace(workspaceId)
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
            Log.d(TAG, "State: $this")
            user?.let {
                setUpDrawer(it)
                activity.drawerAdapter?.setData(
                    it.workspaces.map { userWorkspace ->
                        DrawerAdapter.DrawerWorkspace(
                            userWorkspace,
                            userWorkspace.id == currentWorkspace?.id
                        )
                    }.sortedBy { drawerWorkspace ->
                        drawerWorkspace.workspace.name
                    }
                )
            }

            if (currentWorkspace != null) {
                activity.drawerAdapter?.prevSelectedWorkspaceId = currentWorkspace.id
                createMenu()
                binding.apply {
                    topAppBar.toolbar.title = currentWorkspace.name
                    fabCreateBoard.visibility = View.VISIBLE
                    rvBoards.visibility = View.VISIBLE
                }

                boardsAdapter?.setData(currentWorkspace.boards)

                if (currentWorkspace.boards.isEmpty()) {
                    binding.tvTip.isVisible = true
                    binding.tvTip.text = resources.getString(R.string.empty_workspace_tip)
                } else {
                    binding.tvTip.isVisible = false
                }
            } else {
                binding.apply {
                    topAppBar.toolbar.title = resources.getString(R.string.boards)
                    topAppBar.toolbar.menu.clear()
                    tvTip.text = resources.getString(R.string.workspace_selection_tip)
                    fabCreateBoard.visibility = View.GONE
                }
            }

            binding.loading.root.isVisible = isLoading

            message?.let {
                showToast(it)
                viewModel.messageShown()
            }
        }
    }

    private fun checkUserAuthState() {
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // reload FirebaseUser instance
                viewModel.updateUser()
                val userInfo =
                    viewModel.firebaseUser?.providerData?.first { it.providerId != "firebase" }

                Log.d(
                    TAG,
                    "provider: ${userInfo?.providerId}, isEmailVerified: ${userInfo?.isEmailVerified}"
                )

                if (viewModel.firebaseUser == null) {
                    navController.navigate(R.id.action_userBoardsFragment_to_registrationPromptFragment)
                }

                if (userInfo?.providerId == AuthProvider.EMAIL.providerId && viewModel.firebaseUser?.isEmailVerified == false) {
                    showToast(
                        message = "Complete registration by signing in with ${userInfo.providerId} and verifying your email",
                        context = requireActivity()
                    )
                    navController.navigate(UserBoardsFragmentDirections.actionUserBoardsFragmentToRegistrationPromptFragment())
                }
            }
        }
    }

    private fun setUpDrawer(user: User) {
        with(activity) {
            // set up header layout
            activityMainBinding.headerLayout.apply {
                tvName.text = user.name
                tvEmail.text = user.email
                Glide.with(requireContext())
                    .load(user.profilePicture)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(ivProfilePicture)
            }

            activityMainBinding.btnCreateWorkspace.setOnClickListener {
                buildWorkspaceCreationDialog(user)
            }
        }
    }

    private fun createMenu() {
        with(activity) {
            removeMenuProvider(menuProvider)
            addMenuProvider(
                menuProvider,
                viewLifecycleOwner,
                Lifecycle.State.RESUMED
            )
        }
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
                UserBoardsFragmentDirections.actionUserBoardsFragmentToBoardFragment(
                    boardInfo
                )
            )
        }

        binding.rvBoards.apply {
            adapter = boardsAdapter
            layoutManager = GridLayoutManager(requireContext(), RECYCLERVIEW_BOARDS_COLUMNS)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity.removeMenuProvider(menuProvider)
        boardsAdapter = null
        _binding = null
    }
}