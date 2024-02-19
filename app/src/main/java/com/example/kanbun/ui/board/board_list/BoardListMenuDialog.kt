package com.example.kanbun.ui.board.board_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.kanbun.databinding.BoardListMenuDialogBinding
import com.example.kanbun.domain.model.Board
import com.example.kanbun.domain.model.BoardList
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BoardListMenuDialog : BottomSheetDialogFragment() {
    private var _binding: BoardListMenuDialogBinding? = null
    private val binding: BoardListMenuDialogBinding get() = _binding!!
    private val viewModel: BoardListViewModel by viewModels()
    private lateinit var boardList: BoardList

    companion object {
        fun init(boardList: BoardList): BoardListMenuDialog {
            return BoardListMenuDialog().apply {
                this.boardList = boardList
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BoardListMenuDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnDelete.setOnClickListener {
            viewModel.deleteBoardList(boardList.path, boardList.id) { dismiss() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}