package com.example.kanbun.presentation.root.user_boards

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.GravityCompat
import androidx.core.widget.doOnTextChanged
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
import com.example.kanbun.domain.model.User
import com.example.kanbun.presentation.BaseFragment
import com.example.kanbun.presentation.StateHandler
import com.example.kanbun.presentation.ViewState
import com.example.kanbun.presentation.main_activity.MainActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
            user?.let {
                setUpDrawer(it)
                (requireActivity() as MainActivity).drawerAdapter?.setData(it.workspaces)
            }

            message?.let {
                showToast(it)
                viewModel.messageShown()
            }

            binding.text.text = "Current workspace's boards: ${state.currentWorkspace?.name}"
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

    private fun setUpDrawer(user: User) {
        with(requireActivity() as MainActivity) {
            // set up header layout
            activityMainBinding.headerLayout.apply {
                btnSignOut.setOnClickListener {
                    viewModel.signOutUser(requireContext())
                    navController.navigate(R.id.registrationPromptFragment)
                }

                tvName.text = user.name
                tvEmail.text = user.email
                Glide.with(requireContext())
                    .load(user.profilePicture)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(ivProfilePicture)
            }

            activityMainBinding.btnCreateWorkspace.setOnClickListener {
                buildDialog(user)
            }

            // set drawer's recyler view on item clicked callback
            drawerAdapter?.onItemClickCallback = { workspaceId ->
                viewModel.selectWorkspace(workspaceId)
                activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
    }

    private fun buildDialog(user: User) {
        val editText = EditText(requireContext()).apply {
            hint = "Enter a new workspace name"
        }

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create workspace")
            .setView(editText)
            .setPositiveButton("Create") { dialog, which ->
                viewModel.createWorkspace(editText.text.toString(), user)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }

        val dialog = dialogBuilder.create().apply {
            setOnShowListener {
                val posButton = getButton(AlertDialog.BUTTON_POSITIVE).apply { isEnabled = false }
                editText.doOnTextChanged { text, start, before, count ->
                    posButton.isEnabled = text?.trim().isNullOrEmpty() == false
                }
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}