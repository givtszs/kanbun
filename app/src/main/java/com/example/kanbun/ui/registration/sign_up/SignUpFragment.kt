package com.example.kanbun.ui.registration.sign_up

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.kanbun.R
import com.example.kanbun.common.AuthProvider
import com.example.kanbun.common.ToastMessage
import com.example.kanbun.databinding.FragmentSignUpBinding
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.registration.AuthFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUpFragment : AuthFragment(), StateHandler {
    private var _binding: FragmentSignUpBinding? = null
    private val binding: FragmentSignUpBinding get() = _binding!!
    private val viewModel: SignUpViewModel by viewModels()
    private val args: SignUpFragmentArgs by navArgs()

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
            state.processError(nameError, binding.tfName)
            state.processError(emailError, binding.tfEmail)
            state.processError(passwordError, binding.tfPassword)
            state.processError(confirmationPasswordError, binding.tfConfirmPassword)

            message?.let {
                showToast(it, duration = Toast.LENGTH_LONG)
                viewModel.messageShown()
            }
        }
    }

    override fun clearTextFieldFocus(view: View) {
        binding.tfEmail.clearFocus()
        binding.tfPassword.clearFocus()
        binding.tfConfirmPassword.clearFocus()
        hideKeyboard(view)
    }

    override fun setUpListeners() {
        binding.etEmail.setText(args.email)
        binding.etPassword.setText(args.password)

        setUpTextField(binding.tfName, binding.etName, viewModel)
        setUpTextField(binding.tfEmail, binding.etEmail, viewModel)
        setUpTextField(binding.tfPassword, binding.etPassword, viewModel)
        setUpTextField(binding.tfConfirmPassword, binding.etConfirmPassword, viewModel)

        var job: Job? = null
        binding.btnSignUp.setOnClickListener {
            clearTextFieldFocus(it)
            job?.cancel()
            job = viewModel.signUpWithEmail(
                name = binding.tfName.editText?.text?.trim().toString(),
                email = binding.tfEmail.editText?.text.toString(),
                password = binding.tfPassword.editText?.text.toString(),
                confirmationPassword = binding.tfConfirmPassword.editText?.text.toString(),
                successCallback = {
                    showToast(ToastMessage.SIGN_UP_SUCCESS, context = requireActivity())
                    navController.navigate(R.id.emailVerificationFragment)
                }
            )
        }

        binding.btnSignUpGoogle.setOnClickListener {
            val signInIntent = viewModel.getGoogleSignInClient(requireContext()).signInIntent
            activityResultLauncher.launch(signInIntent)
        }

        binding.btnSignUpGitHub.setOnClickListener {
            viewModel.authWithGitHub(requireActivity()) { firebaseUser ->
                showToast(ToastMessage.SIGN_UP_SUCCESS, context = requireActivity())
                viewModel.saveUserData(firebaseUser, AuthProvider.GITHUB)
                navController.navigate(R.id.action_to_userBoardsFragment)
            }
        }

        binding.tvSignIn.setOnClickListener {
            navController.navigate(
                SignUpFragmentDirections.actionToSignInFragment(
                    email = binding.tfEmail.editText?.text.toString(),
                    password = binding.tfPassword.editText?.text.toString()
                )
            )
        }
    }

    override fun googleAuthCallback(idToken: String?) {
        viewModel.authWithGoogle(idToken) { firebaseUser ->
            showToast(ToastMessage.SIGN_UP_SUCCESS, context = requireActivity())
            viewModel.saveUserData(firebaseUser, AuthProvider.GOOGLE)
            navController.navigate(R.id.action_to_userBoardsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}