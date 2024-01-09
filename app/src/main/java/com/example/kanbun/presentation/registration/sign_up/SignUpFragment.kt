package com.example.kanbun.presentation.registration.sign_up

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.kanbun.R
import com.example.kanbun.common.AuthType
import com.example.kanbun.databinding.FragmentSignUpBinding
import com.example.kanbun.presentation.StateHandler
import com.example.kanbun.presentation.ViewState
import com.example.kanbun.presentation.registration.AuthFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUpFragment : AuthFragment(), StateHandler {
    private var _binding: FragmentSignUpBinding? = null
    private val binding: FragmentSignUpBinding get() = _binding!!
    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.toolbar) { navController.popBackStack(R.id.registrationPromptFragment, false) }
        collectState()
    }

    override fun collectState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.signUpState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.AuthState) {
            if (emailError.isNotEmpty()) {
                showTextFieldError(binding.tfEmail, emailError)
            }

            if (passwordError.isNotEmpty()) {
                showTextFieldError(binding.tfPassword, passwordError)
            }

            if (!confirmationPasswordError.isNullOrEmpty()) {
                showTextFieldError(binding.tfConfirmPassword, confirmationPasswordError)
            }

            message?.let {
                showToast(it, Toast.LENGTH_LONG)
                viewModel.messageShown()
            }
        }
    }

    override fun setUpListeners() {
        binding.etEmail.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) {
                binding.tfEmail.isErrorEnabled = false.also {
                    viewModel.resetEmailError()
                }
            }

        }

        binding.etPassword.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) {
                binding.tfPassword.isErrorEnabled = false.also {
                    viewModel.resetPasswordError()
                }
            }
        }

        binding.etConfirmPassword.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) {
                binding.tfConfirmPassword.isErrorEnabled = false.also {
                    viewModel.resetConfirmationPasswordError()
                }
            }
        }

        var job: Job? = null
        binding.btnSignUp.setOnClickListener {
            clearTextFieldFocus(it)
            job?.cancel()
            job = viewModel.signUpUser(
                email = binding.tfEmail.editText?.text.toString(),
                password = binding.tfPassword.editText?.text.toString(),
                confirmationPassword = binding.tfConfirmPassword.editText?.text.toString(),
                provider = AuthType.EMAIL,
                successCallback = {
//                    showToast(
//                        "Navigate to email verification screen",
//                        Toast.LENGTH_SHORT
//                    )
                    navController.navigate(R.id.emailVerificationFragment)
                }
            )
        }

        binding.tvSignIn.setOnClickListener {
            navController.navigate(R.id.signInFragment)
        }
    }

    override fun clearTextFieldFocus(view: View) {
        binding.tfEmail.clearFocus()
        binding.tfPassword.clearFocus()
        binding.tfConfirmPassword.clearFocus()
        hideKeyboard(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}