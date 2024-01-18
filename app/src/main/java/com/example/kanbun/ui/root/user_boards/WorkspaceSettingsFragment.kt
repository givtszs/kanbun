package com.example.kanbun.ui.root.user_boards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.kanbun.databinding.FragmentWorkspaceSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WorkspaceSettingsFragment : DialogFragment() {
    private var _binding: FragmentWorkspaceSettingsBinding? = null
    private val binding: FragmentWorkspaceSettingsBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkspaceSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
}