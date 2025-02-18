package com.example.kanbun.ui.board.common_adapters

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.kanbun.ui.model.TagUi
import com.example.kanbun.ui.custom_views.TagView

class TagsAdapter(
    private val areItemsClickable: Boolean = false
) : RecyclerView.Adapter<TagsAdapter.ItemTagViewHolder>() {
    var tags: List<TagUi> = emptyList()
        set(value) {
            if (field != value) {
                field = value.sortedBy { it.tag.name }
                Log.d("CreateTaskFragment", "setTags: $field")
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTagViewHolder {
        return ItemTagViewHolder(
            tagView = TagView(context = parent.context, isBig = true)
        ) { position ->
            if (areItemsClickable) {
                tags[position].isSelected = !tags[position].isSelected
                Log.d("CreateTaskFragment", "tag selected: ${tags[position]}")
                notifyItemChanged(position)
            }
        }
    }

    override fun onBindViewHolder(holder: ItemTagViewHolder, position: Int) {
        val tagUi = tags[position]
        holder.tagView.bind(
            tag = tagUi.tag,
            isClickable = areItemsClickable,
            isSelected = tagUi.isSelected
        )
    }

    override fun getItemCount(): Int = tags.size

    class ItemTagViewHolder(
        val tagView: TagView,
        clickAtPosition: (Int) -> Unit
    ) : ViewHolder(tagView) {
        init {
            tagView.setOnCardClickListener {
                clickAtPosition(adapterPosition)
            }
        }
    }
}