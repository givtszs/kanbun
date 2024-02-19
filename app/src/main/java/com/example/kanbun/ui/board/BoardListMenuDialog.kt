package com.example.kanbun.ui.board

import com.example.kanbun.domain.model.BoardList
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.checkerframework.common.subtyping.qual.Bottom

class BoardListMenuDialog : BottomSheetDialogFragment() {
    private lateinit var listName: String

    companion object {
        fun init(listName: String): BoardListMenuDialog {
            return BoardListMenuDialog().apply {
                this.listName = listName
            }
        }
    }
}