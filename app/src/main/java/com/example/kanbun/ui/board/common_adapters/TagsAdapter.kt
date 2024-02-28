package com.example.kanbun.ui.board.common_adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.kanbun.R
import com.example.kanbun.databinding.ItemCreateTagBinding
import com.example.kanbun.ui.model.TagUi
import com.example.kanbun.ui.task_tag.TagView

private const val VIEW_TYPE_TAG = 0
private const val VIEW_TYPE_CREATE_TAG = 1

class TagsAdapter(
    private val areItemsClickable: Boolean = false,
    private val createTags: Boolean = false
) : RecyclerView.Adapter<ViewHolder>() {

    var onCreateTagClicked: () -> Unit = {}

    var tags: List<TagUi> = emptyList()
        set(value) {
            field = value.sortedBy { it.tag.name }
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when {
            createTags && viewType == VIEW_TYPE_CREATE_TAG ->  ItemCreateTagViewHolder(
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_create_tag, parent, false),
                onCreateTagClicked
            )

            viewType == VIEW_TYPE_TAG -> ItemTagViewHolder(
                tagView = TagView(context = parent.context, isBig = true)
            ) { position ->
                if (areItemsClickable) {
                    tags[position].isSelected = !tags[position].isSelected
                    notifyItemChanged(position)
                }
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is ItemTagViewHolder) {
            val tagUi = tags[position]
            holder.tagView.bind(
                tag = tagUi.tag,
                isClickable = areItemsClickable,
                isSelected = tagUi.isSelected
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        Log.d("TagsAdapter", "getItemViewType: position: $position")
        return if (position < tags.size) {
            VIEW_TYPE_TAG
        } else {
            VIEW_TYPE_CREATE_TAG
        }
    }

    override fun getItemCount(): Int =
        if (createTags) {
            tags.size + 1 // +1 for anchor `Create tag` item
        } else {
            tags.size
        }

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

    class ItemCreateTagViewHolder(
        view: View,
        onItemClicked: () -> Unit
    ) : ViewHolder(view) {
        init {
            ItemCreateTagBinding.bind(view).cardCreateTag.setOnClickListener {
                onItemClicked()
            }
        }
    }
}