package com.example.kanbun.ui.board_settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.kanbun.databinding.FragmentBoardSettingsBinding
import com.example.kanbun.domain.model.Board
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.main_activity.MainActivity
import com.google.android.material.appbar.MaterialToolbar

class BoardSettingsFragment : BaseFragment() {
    private var _binding: FragmentBoardSettingsBinding? = null
    private val binding: FragmentBoardSettingsBinding get() = _binding!!
    private val args: BoardSettingsFragmentArgs by navArgs()
    private lateinit var board: Board


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoardSettingsBinding.inflate(inflater, container, false)
        board = args.board
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.toolbar)
    }

    override fun setUpActionBar(toolbar: MaterialToolbar) {
        (requireActivity() as MainActivity).setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            navController.popBackStack()
        }
    }

    override fun setUpListeners() {
        binding.apply {
            etName.setText(board.name)
            etDescription.setText(board.description)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}