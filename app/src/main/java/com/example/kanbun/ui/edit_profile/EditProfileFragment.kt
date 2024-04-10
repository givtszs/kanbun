package com.example.kanbun.ui.edit_profile

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
    private var isUserDataFetched = false

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
        binding.apply {
            btnSave.setOnClickListener {
                viewModel.updateUser(
                    name = binding.etName.text?.trim().toString(),
                    tag = binding.etTag.text?.trim().toString()
                ) {
                    navController.popBackStack()
                }
            }

            etName.doOnTextChanged { text, _, _, _ ->
                if (tfName.isErrorEnabled && !text.isNullOrEmpty()) {
                    tfName.isErrorEnabled = false
                }
            }

            etTag.doOnTextChanged { text, _, _, _ ->
                if (tfTag.isErrorEnabled && !text.isNullOrEmpty()) {
                    tfTag.isErrorEnabled = false
                }
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

                // update to fetch the user data only once
                user?.let { _user ->
                    if (!isUserDataFetched) {
                        loadUserProfilePicture(requireContext(), _user.profilePicture, ivProfilePicture)
                        etName.setText(_user.name)
                        etEmail.setText(_user.email)
                        etTag.setText(_user.tag)
                        isUserDataFetched = true
                    }
                }

                nameError?.let {
                    tfName.error = it
                    tfName.isErrorEnabled = true
                    viewModel.nameErrorShown()
                }

                tagError?.let {
                    tfTag.error = it
                    tfTag.isErrorEnabled = true
                    viewModel.tagErrorShown()
                }

                message?.let {
                    showToast(it)
                    viewModel.messageShown()
                }
            }
        }
    }
}