package com.example.kanbun.presentation.registration.promt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.kanbun.R
import com.example.kanbun.databinding.FragmentRegistrationPromptBinding
import com.example.kanbun.presentation.BaseFragment

class RegistrationPromptFragment : BaseFragment() {
    private var _binding: FragmentRegistrationPromptBinding? = null
    private val binding: FragmentRegistrationPromptBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrationPromptBinding.inflate(inflater, container, false)
        setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary_variant))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnLogIn.setOnClickListener {
            navigate(R.id.logInFragment)
        }

        binding.tvSignUp.setOnClickListener {
            navigate(R.id.signUpFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}