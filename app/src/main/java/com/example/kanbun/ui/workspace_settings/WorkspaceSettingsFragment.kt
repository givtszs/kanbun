package com.example.kanbun.ui.workspace_settings

import android.os.Bundle
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
import com.example.kanbun.common.getColor
import com.example.kanbun.databinding.FragmentWorkspaceSettingsBinding
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.main_activity.MainActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WorkspaceSettingsFragment : BaseFragment(), StateHandler {
    private var _binding: FragmentWorkspaceSettingsBinding? = null
    private val binding: FragmentWorkspaceSettingsBinding get() = _binding!!
    private val viewModel: WorkspaceSettingsViewModel by viewModels()
    private val args: WorkspaceSettingsFragmentArgs by navArgs()
    private lateinit var workspace: Workspace

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
        setStatusBarColor(getColor(requireContext(), R.color.md_theme_light_surface))
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
                        // update members
                    )
                ) {
                    showToast("Workspace settings have been updated", requireActivity())
                    navController.popBackStack()
                }
            }

            btnDeleteWorkspace.setOnClickListener {
                buildConfirmationDialog()
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
//            binding.loading.root.isVisible = isLoading
            binding.deletingState.isVisible = isLoading
        }
    }

    private fun buildConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete ${workspace.name} workspace?")
            .setPositiveButton("Delete") { _, _ ->
//                lifecycleScope.launch {
//                    (requireActivity() as MainActivity).drawerAdapter?.prevSelectedWorkspaceId =
//                        null
//                    viewModel.deleteWorkspace(workspace)
//                    navController.popBackStack()
//                }
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
        _binding = null
    }
}