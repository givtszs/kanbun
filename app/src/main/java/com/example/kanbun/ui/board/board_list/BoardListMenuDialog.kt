package com.example.kanbun.ui.board.board_list

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import com.example.kanbun.R
import com.example.kanbun.databinding.BoardListMenuDialogBinding
import com.example.kanbun.domain.model.BoardList
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BoardListMenuDialog : BottomSheetDialogFragment() {
    private var _binding: BoardListMenuDialogBinding? = null
    private val binding: BoardListMenuDialogBinding get() = _binding!!
    private val viewModel: BoardListViewModel by viewModels()
    private lateinit var boardList: BoardList
    private lateinit var boardLists: List<BoardList>
    private var areOptionsEnabled: Boolean = false

    companion object {
        fun init(
            boardList: BoardList,
            boardLists: List<BoardList>,
            areOptionsEnabled: Boolean
        ): BoardListMenuDialog {
            return BoardListMenuDialog().apply {
                this.boardList = boardList
                this.boardLists = boardLists
                this.areOptionsEnabled = areOptionsEnabled
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

        binding.apply {
            btnDelete.isEnabled = areOptionsEnabled
            btnDelete.setOnClickListener {
                viewModel.deleteBoardList(boardList, boardLists) { dismiss() }
            }

            btnEditName.isEnabled = areOptionsEnabled
            btnEditName.setOnClickListener {
                buildEditNameDialog()
            }
        }
    }

    private fun buildEditNameDialog() {
        val editTextName = EditText(requireContext()).apply {
            setText(boardList.name)
            hint = "Enter list's name"

            // Create layout parameters
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // Set margins (adjust these values as needed)
                val horizontalMarginInPixels =
                    resources.getDimensionPixelSize(R.dimen.alert_dialog_edit_text_horizontal_margin)
                setMargins(horizontalMarginInPixels, 0, horizontalMarginInPixels, 0)
            }

            // Apply layout parameters to the EditText
            this.layoutParams = layoutParams
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit list's name")
            .setView(editTextName)
            .setPositiveButton("Save") { _, _ ->
                viewModel.editBoardListName(
                    newName = editTextName.text.trim().toString(),
                    boardListPath = boardList.path,
                    boardListId = boardList.id
                ) {
                    this@BoardListMenuDialog.dismiss()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                dismiss()
            }
            .create()
            .apply {
                setOnShowListener {
                    val positiveButton = getButton(AlertDialog.BUTTON_POSITIVE)
                    editTextName.doOnTextChanged { text, _, _, _ ->
                        positiveButton.isEnabled = text.isNullOrEmpty() == false
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}