package com.example.kanbun.ui.board

import android.content.ClipData
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

interface DropCallback {
    fun <T : ViewHolder> drop(clipData: ClipData, adapter: RecyclerView.Adapter<T>, position: Int): Boolean
}