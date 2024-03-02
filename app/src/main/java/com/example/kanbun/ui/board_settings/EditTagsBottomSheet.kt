package com.example.kanbun.ui.board_settings

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.common.getColor
import com.example.kanbun.databinding.EditTagsBottomSheetBinding
import com.example.kanbun.databinding.ItemEditTagBinding
import com.example.kanbun.domain.model.Tag
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

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
        editTagsAdapter = EditTagsAdapter(tags)
        binding.rvTags.adapter = editTagsAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        editTagsAdapter = null
    }

    private class EditTagsAdapter(
        private val tags: List<Tag>
    ) : RecyclerView.Adapter<EditTagsAdapter.ItemEditTagViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemEditTagViewHolder {
            return ItemEditTagViewHolder(
                ItemEditTagBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: ItemEditTagViewHolder, position: Int) {
            holder.bind(tags[position])
        }

        override fun getItemCount(): Int = tags.size

        class ItemEditTagViewHolder(
            private val binding: ItemEditTagBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(tag: Tag) {
                binding.apply {
                    tvTagName.text = tag.name
                    val shapeAppearance = ShapeAppearanceModel()
                        .toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, 30f)
                        .build()
                    val shapeDrawable = MaterialShapeDrawable(shapeAppearance)
                    shapeDrawable.fillColor = ColorStateList.valueOf(Color.parseColor(tag.backgroundColor))
                    tvTagName.background = shapeDrawable
                    tvTagName.setTextColor(Color.parseColor(tag.textColor))
//                    tvTagName.setBackgroundColor(Color.parseColor(tag.backgroundColor))
                }
            }
        }
    }
}