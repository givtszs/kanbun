package com.example.kanbun.ui.root.user_boards.workspace_settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.kanbun.databinding.FragmentWorkspaceSettingsBinding
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.ui.main_activity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WorkspaceSettingsFragment : DialogFragment() {
    private var _binding: FragmentWorkspaceSettingsBinding? = null
    private val binding: FragmentWorkspaceSettingsBinding get() = _binding!!
    private val viewModel: WorkspaceSettingsViewModel by viewModels()
    private lateinit var workspace: Workspace

    companion object {
        fun newInstance(workspace: Workspace): WorkspaceSettingsFragment {
            val fragment = WorkspaceSettingsFragment()
            val args = Bundle().apply {
                putParcelable("workspace", workspace)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkspaceSettingsBinding.inflate(inflater, container, false)
        (requireActivity() as MainActivity).activityMainBinding.navBar.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        workspace = arguments?.getParcelable<Workspace>("workspace")
            ?: throw IllegalArgumentException("Argument can't be null")
        setUpActionBar()

        binding.tfName.error = "Workspace name can't be empty"

        binding.etName.doOnTextChanged { text, start, before, count ->
            binding.btnSave.isActivated = text?.trim().isNullOrEmpty() == false
            binding.tfName.isErrorEnabled = text?.trim().isNullOrEmpty() == true
        }
    }

    private fun setUpActionBar() {
//        (requireActivity() as MainActivity).apply {
//            setSupportActionBar(binding.toolbar)
//        }

        binding.tfName.apply {
            editText?.setText(workspace.name)
            isErrorEnabled = false
        }

        binding.btnClose.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.btnSave.setOnClickListener {
            lifecycleScope.launch {
                val updateRes = viewModel.updateWorkspace(
                    workspace = workspace,
                    newName = binding.tfName.editText?.text?.trim().toString()
                )
                if (updateRes.first) {
                    requireActivity().supportFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), updateRes.second, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (requireActivity() as MainActivity).activityMainBinding.navBar.visibility = View.VISIBLE
    }
}