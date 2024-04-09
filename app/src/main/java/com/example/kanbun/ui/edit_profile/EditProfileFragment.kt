package com.example.kanbun.ui.edit_profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.kanbun.common.loadUserProfilePicture
import com.example.kanbun.databinding.FragmentEditProfileBinding
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileFragment : BaseFragment(), StateHandler {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding: FragmentEditProfileBinding get() = _binding!!
    private val viewModel: EditProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.toolbar)
        collectState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun setUpListeners() {
        binding.btnSave.setOnClickListener {
            viewModel.updateUser(
                name = binding.etName.text?.trim().toString(),
                tag = binding.etTag.text?.trim().toString()
            ) {
                navController.popBackStack()
            }
        }
    }

    override fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.editProfileState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with (state as ViewState.EditProfileViewState) {
            binding.apply {
                loading.root.isVisible = isLoading
                user?.let { _user ->
                    loadUserProfilePicture(requireContext(), _user.profilePicture, ivProfilePicture)
                    etName.setText(_user.name)
                    etEmail.setText(_user.email)
                    etTag.setText(_user.tag)
                }

                message?.let {
                    showToast(it)
                    viewModel.messageShown()
                }
            }
        }
    }
}