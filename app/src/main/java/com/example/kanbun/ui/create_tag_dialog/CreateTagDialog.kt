package com.example.kanbun.ui.create_tag_dialog

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.COLOR_GRID_COLUMNS
import com.example.kanbun.common.getColor
import com.example.kanbun.common.tagColors
import com.example.kanbun.databinding.AlertDialogCreateTagBinding
import com.example.kanbun.databinding.ItemColorPreviewBinding
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.ui.custom_views.TagColorView
import com.example.kanbun.ui.custom_views.TagView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CreateTagDialog(
    val context: Context,
    val onDoneClicked: (Tag) -> Unit
) {

    private var binding: AlertDialogCreateTagBinding =
        AlertDialogCreateTagBinding.inflate(
            LayoutInflater.from(context),
            null,
            false

        )
    private var tag: Tag? = null
    private val tagName = MutableStateFlow("")
    private val tagColor = MutableStateFlow(-1)
    private var colorPickerAdapter: ColorPickerAdapter = ColorPickerAdapter { colorId ->
        tagColor.value = colorId
    }

    init {
        binding.etName.doOnTextChanged { text, _, _, _ ->
            tagName.value = text?.trim().toString()
        }
    }

    fun setTag(tag: Tag) {
        this.tag = tag
        binding.etName.setText(tag.name)
        colorPickerAdapter.selectColor(tag.colorId)
        tagColor.value = tag.colorId
    }

    fun buildDialog(
        @StringRes title: Int,
    ) {
        binding.rvTagColors.apply {
            adapter = colorPickerAdapter
            layoutManager = GridLayoutManager(context, COLOR_GRID_COLUMNS)
            itemAnimator = null
        }

        MaterialAlertDialogBuilder(context, R.style.MaterialDialog)
            .setTitle(title)
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton("Done") { _, _ ->
                onDoneClicked(
                    tag?.copy(name = tagName.value, colorId = tagColor.value)
                        ?: Tag(name = tagName.value, colorId = tagColor.value)
                )
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .apply {
                setOnShowListener {
                    val positiveButton = getButton(AlertDialog.BUTTON_POSITIVE).apply {
                        isEnabled = false
                    }

//                    val isNameSet = MutableStateFlow(false)
//                    binding.etName.doOnTextChanged { text, _, _, _ ->
//                        isNameSet.value = !text.isNullOrEmpty()
//                    }
                    // listen to the tag's text and color changes to enable/disable the positive button
                    lifecycleScope.launch {
                        combine(tagColor, tagName) { color, name ->
                            Log.d("CreateTagDialog", "color: $color, name: $name")
                            color != -1 && name.isNotEmpty()
                        }.collectLatest {
                            positiveButton.isEnabled = it
                        }
                    }
                }
            }
            .show()
    }

    private class ColorPickerAdapter(private val onItemClicked: (Int) -> Unit) :
        RecyclerView.Adapter<ColorPickerAdapter.ItemColorPreviewViewHolder>() {

        init {
            ItemColorPreviewViewHolder.prevSelectedPos = -1
        }

        fun selectColor(position: Int) {
            notifyItemChanged(ItemColorPreviewViewHolder.prevSelectedPos)
            ItemColorPreviewViewHolder.prevSelectedPos = position
            // "select" current color item
            notifyItemChanged(position)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ItemColorPreviewViewHolder {
            return ItemColorPreviewViewHolder(
                ItemColorPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ) { position ->
                if (ItemColorPreviewViewHolder.prevSelectedPos != position) {
//                    // "unselect" previous color item
//                    notifyItemChanged(ItemColorPreviewViewHolder.prevSelectedPos)
//                    ItemColorPreviewViewHolder.prevSelectedPos = position
//                    // "select" current color item
//                    notifyItemChanged(position)
                    selectColor(position)
                    onItemClicked(position)
                }
            }
        }

        override fun onBindViewHolder(holder: ItemColorPreviewViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount(): Int = tagColors.size

        class ItemColorPreviewViewHolder(
            private val binding: ItemColorPreviewBinding,
            private val clickAtPosition: (Int) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            companion object {
                var prevSelectedPos = -1
            }

            init {
                binding.cardColor.setOnClickListener {
                    if (adapterPosition != prevSelectedPos) {
                        clickAtPosition(adapterPosition)
                    }
                }
            }

            fun bind(colorId: Int) {
                binding.apply {
                    cardColor.isSelected = adapterPosition == prevSelectedPos
                    cardColor.setCardBackgroundColor(getColor(itemView.context, tagColors[colorId] ?: R.color.md_theme_light_primary))
                }
            }
        }
    }
}