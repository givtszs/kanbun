package com.example.kanbun.presentation.registration.sign_in

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kanbun.databinding.FragmentSignInBinding
import com.example.kanbun.presentation.BaseFragment
import com.example.kanbun.presentation.registration.AuthFragment
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInFragment : AuthFragment() {
    private var _binding: FragmentSignInBinding? = null
    private val binding: FragmentSignInBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setUpListeners() {
//        TODO("Not yet implemented")
    }

    override fun clearTextFieldFocus(view: View) {
        TODO("Not yet implemented")
    }

    override fun showTextFieldError(input: TextInputLayout, message: String) {
        TODO("Not yet implemented")
    }
}