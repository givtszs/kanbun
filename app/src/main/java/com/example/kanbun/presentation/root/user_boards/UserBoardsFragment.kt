package com.example.kanbun.presentation.root.user_boards

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.kanbun.R
import com.example.kanbun.common.AuthProvider
import com.example.kanbun.databinding.FragmentUserBoardsBinding
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.presentation.BaseFragment
import com.example.kanbun.presentation.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class UserBoardsFragment : BaseFragment() {
    private var _binding: FragmentUserBoardsBinding? = null
    private val binding: FragmentUserBoardsBinding get() = _binding!!
    private var user = Firebase.auth.currentUser

    @Inject
    lateinit var firestoreRepository: FirestoreRepository

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
        addOnBackPressedAction { requireActivity().finish() }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                user?.reload()?.await()
                val userInfo = user?.providerData?.first { it.providerId != "firebase" }
                Log.d(
                    "UserBoardsFragm",
                    "provider: ${userInfo?.providerId}, isEmailVerified: ${userInfo?.isEmailVerified}"
                )
                if (user == null) {
                    navController.navigate(R.id.action_userBoardsFragment_to_registrationPromptFragment)
                } else if (userInfo?.providerId == AuthProvider.EMAIL.providerId && user?.isEmailVerified == false) {
                    showToast(
                        message = "Complete registration by signing in with ${userInfo.providerId} and verifying your email",
                        context = requireActivity()
                    )
                    navController.navigate(UserBoardsFragmentDirections.actionUserBoardsFragmentToRegistrationPromptFragment())
                }
            }
        }
    }

    override fun setUpListeners() {
        binding.navView.setupWithNavController(navController)

        binding.btnSignOut.setOnClickListener {
            Firebase.auth.signOut()
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("723106455668-7apee9lsea93gpi66cjkoiom258i30e2.apps.googleusercontent.com")
                .requestEmail()
                .requestProfile()
                .build()

            val signInClient = GoogleSignIn.getClient(requireContext(), signInOptions)
            signInClient.signOut()
            navController.navigate(R.id.action_userBoardsFragment_to_registrationPromptFragment)
        }
    }

    override fun setUpActionBar(toolbar: MaterialToolbar) {
        val config = AppBarConfiguration(navController.graph, binding.root)
        (requireActivity() as MainActivity).apply {
            appBarConfiguration = config
            setSupportActionBar(binding.toolbar)
            setupActionBarWithNavController(navController, appBarConfiguration)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}