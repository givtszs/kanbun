package com.example.kanbun.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kanbun.R
import com.example.kanbun.common.getColor
import com.example.kanbun.databinding.FragmentSettingsBinding
import com.example.kanbun.ui.BaseFragment

class SettingsFragment : BaseFragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding: FragmentSettingsBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.toolbar)
        setStatusBarColor(getColor(requireContext(), R.color.background_light))
        setNavigationBarColor(getColor(requireContext(), R.color.background_light))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun setUpListeners() {
        binding.prefEditProfile.setOnPreferenceClickListener {
            navController.navigate(R.id.action_settingsFragment_to_editProfileFragment)
        }
    }
}