package com.example.kanbun.presentation.registration.sign_up

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.kanbun.R
import com.example.kanbun.common.AuthType
import com.example.kanbun.databinding.FragmentSignUpBinding
import com.example.kanbun.presentation.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUpFragment : BaseFragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding: FragmentSignUpBinding get() = _binding!!
    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.white))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collectState()
        restoreState()
        setUpListeners()
    }

    private fun collectState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.signUpState.collectLatest {
                    processViewState(it)
                }
            }
        }
    }

    private fun restoreState() {
        binding.etEmail.setText(viewModel.userEmail)
    }

    private fun setUpListeners() {
        binding.etEmail.doOnTextChanged { text, _, _, _ ->
            viewModel.userEmail = text.toString()
        }

        binding.btnSignUp.setOnClickListener {
            viewModel.registerUser(
                email = binding.tfEmail.editText?.text.toString(),
                password = binding.tfPassword.editText?.text.toString(),
                confirmationPassword = binding.tfConfirmPassword.editText?.text.toString(),
                provider = AuthType.EMAIL,
                successCallback = { showToast("Navigate to email verification screen", Toast.LENGTH_SHORT) }
            )
        }

        binding.tvLogIn.setOnClickListener {
            navigate(R.id.logInFragment)
        }
    }

    private fun processViewState(viewState: SignUpViewState) {
        with (viewState) {
            emailError?.let {
                binding.tfEmail.apply {
                    error = it
                    isErrorEnabled = true
                }
            }

            passwordError?.let {
                binding.tfPassword.apply {
                    error = it
                    isErrorEnabled = true
                }
            }

            confirmationPasswordError?.let {
                binding.tfConfirmPassword.apply {
                    error = it
                    isErrorEnabled = true
                }
            }

            message?.let {
                showToast(it, Toast.LENGTH_LONG)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}