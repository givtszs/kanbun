package com.example.kanbun.ui.shared

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.kanbun.common.TAG
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.domain.model.User

class SharedBoardViewModel : ViewModel() {

    var boardMembers: List<User> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                Log.d(TAG, "Members: $field")
            }
        }

    var tags: List<Tag> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                Log.d(TAG, "Tags: $field")
            }
        }
}