package com.example.kanbun.presentation.registration.email_verification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.kanbun.R
import com.example.kanbun.common.ToastMessage
import com.example.kanbun.databinding.FragmentEmailVerificationBinding
import com.example.kanbun.presentation.BaseFragment
import com.example.kanbun.presentation.StateHandler
import com.example.kanbun.presentation.ViewState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EmailVerificationFragment : BaseFragment(), StateHandler {
    private var _binding: FragmentEmailVerificationBinding? = null
    private val binding: FragmentEmailVerificationBinding get() = _binding!!
    private val viewModel: EmailVerificationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmailVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.white))
        setUpActionBar(binding.toolbar)
        setUpListeners()
        collectState()
        waitForEmailVerification()
    }

    override fun setUpListeners() {
        binding.tvSubtitle.text =
            resources.getString(R.string.subtitle_confirm_email, viewModel.getUserInfo()?.email)

        binding.btnResendEmail.setOnClickListener {
            viewModel.sendVerificationEmail(resend = true)
        }
    }

    override fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.emailVerificationState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.EmailVerificationState) {
            binding.btnResendEmail.isEnabled = isResendAvailable == true
            binding.tvCountdown.isVisible = isResendAvailable == false

            if (countdownMillis != 0) {
                binding.tvCountdown.text = resources.getString(
                    R.string.try_again_countdown,
                    countdownMillis
                )
            }

            if (message != null) {
                showToast(message, duration = Toast.LENGTH_LONG)
                viewModel.messageShown()
            }
        }
    }

    private fun waitForEmailVerification() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    // get the recent changes in the user account
                    viewModel.updateUser()
                    delay(1000L)
                    if (viewModel.user?.isEmailVerified == true) {
                        showToast(ToastMessage.EMAIL_VERIFIED)
                        navController.navigate(R.id.action_emailVerificationFragment_to_userBoardsFragment)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}