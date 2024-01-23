package com.example.kanbun.ui.registration.sign_in

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
import com.example.kanbun.databinding.FragmentSignInBinding
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.registration.AuthFragment
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
        setUpActionBar(binding.topAppBar.toolbar)
        collectState()
    }

    override fun clearTextFieldFocus(view: View) {
        binding.tfEmail.clearFocus()
        binding.tfPassword.clearFocus()
        hideKeyboard(view)
    }

    override fun setUpListeners() {
        binding.etEmail.setText(args.email)
        binding.etPassword.setText(args.password)

        setUpTextField(binding.tfEmail, binding.etEmail, viewModel)
        setUpTextField(binding.tfPassword, binding.etPassword, viewModel)

        var job: Job? = null
        binding.btnSignIn.setOnClickListener { view ->
            clearTextFieldFocus(view)
            job?.cancel()
            job = viewModel.signInWithEmail(
                email = binding.tfEmail.editText?.text.toString(),
                password = binding.tfPassword.editText?.text.toString(),
                successCallback = { firebaseUser ->
                    showToast(ToastMessage.SIGN_IN_SUCCESS, context = requireActivity())
                    if (!firebaseUser.isEmailVerified) {
                        navController.navigate(R.id.emailVerificationFragment)
                    } else {
                        navController.navigate(R.id.action_to_userBoardsFragment)
                    }
                }
            )
        }

        binding.btnSignInGoogle.setOnClickListener {
            val signInIntent = viewModel.getGoogleSignInClient(requireContext()).signInIntent
            activityResultLauncher.launch(signInIntent)
        }

        binding.btnSignInGitHub.setOnClickListener {
            viewModel.authWithGitHub(requireActivity()) { firebaseUser ->
                showToast(ToastMessage.SIGN_IN_SUCCESS, context = requireActivity())
                viewModel.saveUserData(firebaseUser, AuthProvider.GITHUB)
                navController.navigate(R.id.action_to_userBoardsFragment)
            }
        }

        binding.tvSignUp.setOnClickListener {
            navController.navigate(
                SignInFragmentDirections.actionToSignUpFragment(
                    email = binding.tfEmail.editText?.text?.trim().toString(),
                    password = binding.tfPassword.editText?.text?.trim().toString()
                )
            )
        }
    }

    override fun googleAuthCallback(idToken: String?) {
        viewModel.authWithGoogle(idToken) { firebaseUser ->
            showToast(ToastMessage.SIGN_IN_SUCCESS, context = requireActivity())
            viewModel.saveUserData(firebaseUser, AuthProvider.GOOGLE)
            navController.navigate(R.id.action_to_userBoardsFragment)
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
            state.processError(emailError, binding.tfEmail)
            state.processError(passwordError, binding.tfPassword)

            message?.let {
                showToast(it, duration = Toast.LENGTH_LONG)
                viewModel.messageShown()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}