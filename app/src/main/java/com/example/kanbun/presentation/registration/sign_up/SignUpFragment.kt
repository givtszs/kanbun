package com.example.kanbun.presentation.registration.sign_up

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUpFragment : BaseFragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding: FragmentSignUpBinding get() = _binding!!
    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(owner = this@SignUpFragment, onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navController.navigateUp()
            }
        })
    }

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
        setUpActionBar(binding.toolbar)
        setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.white))
        collectState()
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

    private fun setUpListeners() {
        binding.etEmail.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) {
                binding.tfEmail.isErrorEnabled = false.also {
                    viewModel.emailError = null
                }
            }
        }

        binding.etPassword.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) {
                binding.tfPassword.isErrorEnabled = false.also {
                    viewModel.passwordError = null
                }
            }
        }

        binding.etConfirmPassword.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) {
                binding.tfConfirmPassword.isErrorEnabled = false.also {
                    viewModel.confirmationPasswordError = null
                }
            }
        }

        var job: Job? = null
        binding.btnSignUp.setOnClickListener {
            clearFocus(it)
            job?.cancel()
            job = viewModel.registerUser(
                email = binding.tfEmail.editText?.text.toString(),
                password = binding.tfPassword.editText?.text.toString(),
                confirmationPassword = binding.tfConfirmPassword.editText?.text.toString(),
                provider = AuthType.EMAIL,
                successCallback = {
                    showToast(
                        "Navigate to email verification screen",
                        Toast.LENGTH_SHORT
                    )
                }
            )
        }

        binding.tvLogIn.setOnClickListener {
            navController.navigate(R.id.logInFragment)
        }
    }

    private fun clearFocus(view: View) {
        binding.tfEmail.clearFocus()
        binding.tfPassword.clearFocus()
        binding.tfConfirmPassword.clearFocus()
        // hide keyboard
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(view.applicationWindowToken, 0)
        }
    }

    private fun processViewState(viewState: SignUpViewState) {
        with(viewState) {
            emailError?.let {
                showError(binding.tfEmail, it)
            }

            passwordError?.let {
                showError(binding.tfPassword, it)
            }

            confirmationPasswordError?.let {
                showError(binding.tfConfirmPassword, it)
            }

            message?.let {
                showToast(it, Toast.LENGTH_LONG)
                viewModel.messageShown()
            }
        }
    }

    private fun showError(input: TextInputLayout, message: String) {
        input.apply {
            error = message
            isErrorEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}