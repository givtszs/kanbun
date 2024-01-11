package com.example.kanbun.presentation.root.user_boards

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kanbun.R
import com.example.kanbun.databinding.FragmentRootUserBoardsBinding
import com.example.kanbun.presentation.BaseFragment
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class UserBoardsFragment : BaseFragment() {
    private var _binding: FragmentRootUserBoardsBinding? = null
    private val binding: FragmentRootUserBoardsBinding get() = _binding!!
    private var user = Firebase.auth.currentUser

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRootUserBoardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addOnBackPressedAction { requireActivity().finish() }

        if (user == null) {
            navController.navigate(R.id.action_userBoardsFragment_to_registrationPromptFragment)
        } else if (user?.isEmailVerified == false) {
            navController.navigate(UserBoardsFragmentDirections.actionUserBoardsFragmentToEmailVerificationFragment())
        }
    }

    override fun setUpListeners() {
        binding.tvUserInfo.text = "${user?.email}, ${user?.isEmailVerified}, ${user?.displayName}, ${user?.photoUrl}"
        binding.btnSignOut.setOnClickListener {
            Firebase.auth.signOut()
            navController.navigate(R.id.registrationPromptFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}