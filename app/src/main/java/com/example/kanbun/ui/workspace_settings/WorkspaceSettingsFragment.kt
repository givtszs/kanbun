package com.example.kanbun.ui.workspace_settings

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
import com.example.kanbun.common.Role
import com.example.kanbun.common.getColor
import com.example.kanbun.databinding.FragmentWorkspaceSettingsBinding
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.main_activity.MainActivity
import com.example.kanbun.ui.manage_members.SearchUsersAdapter
import com.example.kanbun.ui.manage_members.MembersAdapter
import com.example.kanbun.ui.members.MembersBottomSheet
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WorkspaceSettingsFragment : BaseFragment(), StateHandler {
    private val TAG = "WorkspaceSettingsFragment"

    private var _binding: FragmentWorkspaceSettingsBinding? = null
    private val binding: FragmentWorkspaceSettingsBinding get() = _binding!!
    private val viewModel: WorkspaceSettingsViewModel by viewModels()
    private val args: WorkspaceSettingsFragmentArgs by navArgs()
    private lateinit var workspace: Workspace
    private var searchUsersAdapter: SearchUsersAdapter? = null
    private var workspaceMembersAdapter: MembersAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkspaceSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        workspace = args.workspace
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.toolbar)
        setUpAdapters()
        setStatusBarColor(getColor(requireContext(), R.color.md_theme_light_surface))
        viewModel.init(workspace.owner, workspace.members)
        collectState()
    }

    /**
     * Overrides the parent implementation to set up a custom `Up` button.
     *
     * @param toolbar the toolbar to configure
     */
    override fun setUpActionBar(toolbar: MaterialToolbar) {
        (requireActivity() as MainActivity).setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            navController.popBackStack()
        }
    }

    override fun setUpListeners() {
        binding.apply {
            tfName.apply {
                editText?.setText(workspace.name)
            }

            etName.doOnTextChanged { text, _, _, _ ->
                if (!text.isNullOrEmpty()) {
                    tfName.isErrorEnabled = false
                }
            }

            var searchJob: Job? = null
            etSearchUser.doOnTextChanged { text, _, _, _ ->
                searchJob?.cancel()
                if (!text.isNullOrEmpty() && text.length >= 3) {
                    Log.d(TAG, "searchUser: $text")
                    searchJob = viewModel.searchUser(text.toString())
                } else {
                    Log.d(TAG, "searchUser: call resetFoundUsers")
                    viewModel.resetFoundUsers()
                }
            }

            btnSave.setOnClickListener {
                val name = tfName.editText?.text?.trim().toString()
                if (name.isEmpty()) {
                    tfName.apply {
                        error = "Workspace name can't be empty"
                        isErrorEnabled = true
                    }
                    return@setOnClickListener
                }

                viewModel.updateWorkspace(
                    oldWorkspace = workspace,
                    newWorkspace = workspace.copy(
                        name = name,
                        members = viewModel.workspaceSettingsState.value.members.map {
                            Workspace.WorkspaceMember(
                                id = it.user.id,
                                role = it.role as Role.Workspace
                            )
                        }
                    )
                ) {
                    showToast("Workspace settings have been updated", requireActivity())
                    navController.popBackStack()
                }
            }

            btnDeleteWorkspace.setOnClickListener {
                buildConfirmationDialog()
            }

            btnViewAllMembers.setOnClickListener {
                val membersBottomSheet =
                    MembersBottomSheet.init(viewModel.workspaceSettingsState.value.members) { members ->
                        viewModel.setMembers(members)
                    }
                membersBottomSheet.show(childFragmentManager, "workspace_members")
            }
        }
    }

    override fun collectState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.workspaceSettingsState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.WorkspaceSettingsViewState) {
            binding.apply {
                loading.root.isVisible = isLoading
                deletingState.isVisible = isLoading
                message?.let {
                    showToast(it)
                    viewModel.messageShown()
                }

                rvFoundUsers.isVisible = foundUsers != null
                foundUsers?.let { users ->
                    searchUsersAdapter?.users = users
                }
                workspaceMembersAdapter?.members = members.map { it.user }
            }
        }
    }

    private fun setUpAdapters() {
        searchUsersAdapter = SearchUsersAdapter { user ->
            showToast("Clicked on ${user.tag}")
            if (user.id != workspace.owner) {
                viewModel.addMember(user)
            }
        }
        binding.rvFoundUsers.adapter = searchUsersAdapter

        workspaceMembersAdapter = MembersAdapter(ownerId = workspace.owner) { member ->
            viewModel.removeMember(member)
        }
        binding.rvMembers.adapter = workspaceMembersAdapter
    }

    private fun buildConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete ${workspace.name} workspace?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteWorkspaceCloudFn(workspace) {
                    navController.popBackStack()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchUsersAdapter = null
        workspaceMembersAdapter = null
        _binding = null
    }
}