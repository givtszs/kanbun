package com.example.kanbun.presentation.root.user_boards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kanbun.R
import com.example.kanbun.databinding.FragmentRootUserBoardsBinding
import com.example.kanbun.domain.repository.FirestoreRepository
import com.example.kanbun.presentation.BaseFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UserBoardsFragment : BaseFragment() {
    private var _binding: FragmentRootUserBoardsBinding? = null
    private val binding: FragmentRootUserBoardsBinding get() = _binding!!
    private var user = Firebase.auth.currentUser

    @Inject
    lateinit var firestoreRepository: FirestoreRepository

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
//        TODO("Not yet implemented")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}