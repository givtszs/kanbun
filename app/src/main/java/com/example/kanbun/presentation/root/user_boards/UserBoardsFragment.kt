package com.example.kanbun.presentation.root.user_boards

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.ui.setupActionBarWithNavController
import com.bumptech.glide.Glide
import com.example.kanbun.R
import com.example.kanbun.common.AuthProvider
import com.example.kanbun.common.getColor
import com.example.kanbun.databinding.FragmentUserBoardsBinding
import com.example.kanbun.presentation.BaseFragment
import com.example.kanbun.presentation.StateHandler
import com.example.kanbun.presentation.ViewState
import com.example.kanbun.presentation.main_activity.MainActivity
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserBoardsFragment : BaseFragment(), StateHandler {
    private var _binding: FragmentUserBoardsBinding? = null
    private val binding: FragmentUserBoardsBinding get() = _binding!!

    private val viewModel: UserBoardsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBoardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.toolbar)
        setStatusBarColor(getColor(requireContext(), R.color.md_theme_light_surface))
        addOnBackPressedAction { requireActivity().finish() }
        checkUserAuthState()
        collectState()
    }

    override fun setUpListeners() {

    }

    override fun setUpActionBar(toolbar: MaterialToolbar) {
        (requireActivity() as MainActivity).apply {
            setSupportActionBar(toolbar)
            setupActionBarWithNavController(navController, appBarConfiguration)
        }
    }

    override fun collectState() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.userBoardsState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.UserBoardsViewState) {
            messanger.message?.let { message ->
                showToast(message)
                messanger.messageShown()
            }

            user?.let {
                setUpDrawerHeader(it.name, it.email, it.profilePicture)
            }
        }
    }

    private fun checkUserAuthState() {
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateUser()
                val userInfo =
                    viewModel.firebaseUser?.providerData?.first { it.providerId != "firebase" }
                Log.d(
                    "UserBoardsFragm",
                    "provider: ${userInfo?.providerId}, isEmailVerified: ${userInfo?.isEmailVerified}"
                )
                if (viewModel.firebaseUser == null) {
                    navController.navigate(R.id.action_userBoardsFragment_to_registrationPromptFragment)
                } else if (userInfo?.providerId == AuthProvider.EMAIL.providerId && viewModel.firebaseUser?.isEmailVerified == false) {
                    showToast(
                        message = "Complete registration by signing in with ${userInfo.providerId} and verifying your email",
                        context = requireActivity()
                    )
                    navController.navigate(UserBoardsFragmentDirections.actionUserBoardsFragmentToRegistrationPromptFragment())
                }
            }
        }
    }

    private fun setUpDrawerHeader(name: String?, email: String?, profilePic: String?) {
        with(requireActivity() as MainActivity) {
            activityMainBinding.headerLayout.apply {
                btnSignOut.setOnClickListener {
                    viewModel.signOutUser(requireContext())
                    navController.navigate(R.id.registrationPromptFragment)
                }

                tvName.text = name
                tvEmail.text = email
                Glide.with(requireContext())
                    .load(profilePic)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(ivProfilePicture)

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}