package com.example.kanbun.ui.root.user_boards.workspace_settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.example.kanbun.R
import com.example.kanbun.common.getColor
import com.example.kanbun.databinding.FragmentWorkspaceSettingsBinding
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.main_activity.MainActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WorkspaceSettingsFragment : BaseFragment() {
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
    }

    override fun setUpActionBar(toolbar: MaterialToolbar) {
        (requireActivity() as MainActivity).apply {
            toolbar.menu.clear()
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun setUpListeners() {
        with(binding) {
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

                if (name != workspace.name) {
                    lifecycleScope.launch {
                        val updateResult = viewModel.updateWorkspaceName(
                            workspace = workspace,
                            newName = name
                        )

                        showToast(updateResult.second, context = requireActivity())
                        if (updateResult.first) {
                            navController.popBackStack()
                        }
                    }
                }
            }

            btnDeleteWorkspace.setOnClickListener {
                buildConfirmationDialog()
            }
        }
    }

    private fun buildConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete ${workspace.name} workspace?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    (requireActivity() as MainActivity).drawerAdapter?.prevSelectedWorkspaceId = null
                    viewModel.deleteWorkspace(workspace)
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