package com.example.kanbun.ui.board.common_adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.getColor
import com.example.kanbun.databinding.ItemTaskTagBinding
import com.example.kanbun.ui.model.TagUi

class TagsAdapter(private val areItemsClickable: Boolean = false) : RecyclerView.Adapter<TagsAdapter.ItemTagViewHolder>() {

    var tags: List<TagUi> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTagViewHolder {
        return ItemTagViewHolder(
            ItemTaskTagBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        ) { position ->
            if (areItemsClickable) {
                tags[position].isSelected = !tags[position].isSelected
                notifyItemChanged(position)
            }
        }
    }

    override fun onBindViewHolder(holder: ItemTagViewHolder, position: Int) {
        holder.bind(tags[position])
    }

    override fun getItemCount(): Int = tags.size

    class ItemTagViewHolder(
        private val binding: ItemTaskTagBinding,
        private val clickAtPosition: (Int) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {


        init {
            binding.cardTag.setOnClickListener {
                clickAtPosition(adapterPosition)
            }
        }

        fun bind(tagUi: TagUi) {
            binding.apply {
                tvTag.text = tagUi.tag.name
                tvTag.setTextColor(Color.parseColor(tagUi.tag.textColor))
                cardTag.setCardBackgroundColor(Color.parseColor(tagUi.tag.backgroundColor))

                cardTag.strokeColor = if (tagUi.isSelected) {
                    getColor(itemView.context, R.color.md_theme_light_primary)
                } else {
                    getColor(itemView.context, R.color.white)
                }
            }
        }
    }
}