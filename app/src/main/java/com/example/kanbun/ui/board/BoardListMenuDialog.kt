package com.example.kanbun.ui.board

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kanbun.databinding.BoardListMenuDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BoardListMenuDialog : BottomSheetDialogFragment() {
    private var _binding: BoardListMenuDialogBinding? = null
    private val binding: BoardListMenuDialogBinding get() = _binding!!
    private lateinit var listName: String

    companion object {
        fun init(listName: String): BoardListMenuDialog {
            return BoardListMenuDialog().apply {
                this.listName = listName
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

//        binding.etName.setText(listName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}