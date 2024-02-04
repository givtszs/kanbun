package com.example.kanbun.ui.board

import android.content.ClipData
import android.view.DragEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * Interface definition for a callback to be invoked when a dragged view has been released
 */
interface DropCallback {
    /**
     * Called when a dragged view has been released and the [DragEvent.ACTION_DROP] event has been sent
     *
     * @param clipData the data of the dragged view
     * @param adapter the [RecyclerView.Adapter] the dragged view has been dropped into
     * @param position the drop position of the dragged view in the [RecyclerView] list
     * @return `true` if the drop action was handled successfully; `false` otherwise
     */
    fun <T : ViewHolder> drop(clipData: ClipData, adapter: RecyclerView.Adapter<T>, position: Int): Boolean
}