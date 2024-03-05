package com.example.kanbun.ui.board_settings

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.EditTagsBottomSheetBinding
import com.example.kanbun.databinding.ItemEditTagBinding
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.ui.create_tag_dialog.CreateTagDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EditTagsBottomSheet : BottomSheetDialogFragment() {
    private var _binding: EditTagsBottomSheetBinding? = null
    private val binding: EditTagsBottomSheetBinding get() = _binding!!
    private var editTagsAdapter: EditTagsAdapter? = null
    private lateinit var tags: List<Tag>

    companion object {
        fun init(tags: List<Tag>): EditTagsBottomSheet {
            return EditTagsBottomSheet().apply {
                this.tags = tags
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = EditTagsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("EditTags", "tags: $tags")
        editTagsAdapter = EditTagsAdapter(tags.sortedBy { it.name }) { tag ->
            // edit tag
            val tagEditor = CreateTagDialog(requireContext()) {
                // update tag
            }
            tagEditor.setTag(tag)
            tagEditor.show()
        }
        binding.rvTags.adapter = editTagsAdapter

        binding.btnCreateTag.setOnClickListener {
            val createTagDialog = CreateTagDialog(requireContext()) {
                // create tag
            }
            createTagDialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        editTagsAdapter = null
    }

    private class EditTagsAdapter(
        private val tags: List<Tag>,
        private val onItemClicked: (Tag) -> Unit
    ) : RecyclerView.Adapter<EditTagsAdapter.ItemEditTagViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemEditTagViewHolder {
            return ItemEditTagViewHolder(
                ItemEditTagBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            ) { position ->
                onItemClicked(tags[position])
            }
        }

        override fun onBindViewHolder(holder: ItemEditTagViewHolder, position: Int) {
            holder.bind(tags[position])
        }

        override fun getItemCount(): Int = tags.size

        class ItemEditTagViewHolder(
            private val binding: ItemEditTagBinding,
            clickAtPosition: (Int) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.cardBackground.setOnClickListener {
                    clickAtPosition(adapterPosition)
                }
            }

            fun bind(tag: Tag) {
                binding.apply {
                    tvTagName.text = tag.name
                    tvTagName.setTextColor(Color.parseColor(tag.color))
                    cardBackground.setCardBackgroundColor(Color.parseColor(tag.getBackgroundColor()))
                }
            }
        }
    }
}