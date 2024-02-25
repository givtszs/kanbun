package com.example.kanbun.ui.board_settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
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
    private val viewModel: BoardSettingsViewModel by viewModels()

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
            btnDeleteBoard.setOnClickListener {
                viewModel.deleteBoard(board.id, board.workspace.id) {
                    navController.popBackStack()
                }
            }

            btnSave.setOnClickListener {
                // TODO: figure out updates for the tags, members and cover
                val newBoard = board.copy(
                    name = etName.text?.trim().toString(),
                    description = etDescription.text?.trim().toString()
                )

                if (newBoard == board) {
                    showToast("No updates")
                    return@setOnClickListener
                }

                viewModel.updateBoard(newBoard) {
                    navController.popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}