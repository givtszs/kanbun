package com.example.kanbun.presentation.registration.sign_in

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.kanbun.R
import com.example.kanbun.common.AuthType
import com.example.kanbun.databinding.FragmentSignInBinding
import com.example.kanbun.presentation.StateHandler
import com.example.kanbun.presentation.ViewState
import com.example.kanbun.presentation.registration.AuthFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignInFragment : AuthFragment(), StateHandler {
    private var _binding: FragmentSignInBinding? = null
    private val binding: FragmentSignInBinding get() = _binding!!
    private val viewModel: SignInViewModel by viewModels()
    private val args: SignInFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.toolbar) {
            navController.popBackStack(R.id.registrationPromptFragment, false)
        }
        collectState()
    }

    override fun setUpListeners() {
        binding.etEmail.setText(args.email)
        binding.etPassword.setText(args.password)

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

        var job: Job? = null
        binding.btnSignIn.setOnClickListener {
            clearTextFieldFocus(it)
            job?.cancel()
            job = viewModel.signInUser(
                email = binding.tfEmail.editText?.text.toString(),
                password = binding.tfPassword.editText?.text.toString(),
                provider = AuthType.EMAIL,
                successCallback = { user -> authSuccessCallback(user) }
            )
        }

        binding.btnSignInGoogle.setOnClickListener {
            val signInIntent = viewModel.getGoogleSignInClient(requireContext()).signInIntent
            activityResultLauncher.launch(signInIntent)
        }

        binding.btnSignInGitHub.setOnClickListener {
            viewModel.authWithGitHub(requireActivity()) { user ->authSuccessCallback(user) }
        }

        binding.tvSignUp.setOnClickListener {
            navController.navigate(
                SignInFragmentDirections.actionSignInFragmentToSignUpFragment(
                    email = binding.tfEmail.editText?.text.toString(),
                    password = binding.tfPassword.editText?.text.toString()
                )
            )
        }
    }

    override fun googleAuthCallback(idToken: String?) {
        viewModel.authWithGoogle(idToken) {
            Log.d("SignInFragment", "isUserVerified: ${it.isEmailVerified}")
            showToast("SUCCESSFULLY SIGNED IN!")
        }
    }

    override fun collectState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.signInState.collectLatest {
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

            message?.let {
                showToast(it, Toast.LENGTH_LONG)
                viewModel.messageShown()
            }
        }
    }

    override fun clearTextFieldFocus(view: View) {
        binding.tfEmail.clearFocus()
        binding.tfPassword.clearFocus()
        hideKeyboard(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}