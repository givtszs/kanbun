package com.example.kanbun.ui.create_tag_dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.defaultTagColors
import com.example.kanbun.common.getColor
import com.example.kanbun.databinding.AlertDialogCreateTagBinding
import com.example.kanbun.databinding.ItemColorPreviewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CreateTagDialog(
    val context: Context,
    val createTag: (tagColor: String, tagName: String) -> Unit
) {
    private var binding: AlertDialogCreateTagBinding =
        AlertDialogCreateTagBinding.inflate(
            LayoutInflater.from(context),
            null,
            false

        )

    private val tagColor = MutableStateFlow("")
    private var colorPickerAdapter: ColorPickerAdapter = ColorPickerAdapter { colorId ->
        tagColor.value = colorId
    }

    fun create() {
        binding.rvTagColors.apply {
            adapter = colorPickerAdapter
            layoutManager = GridLayoutManager(context, 4)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Create tag")
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton("Create") { _, _ ->
                createTag(tagColor.value, binding.etName.text?.trim().toString())
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
                    val isNameSet = MutableStateFlow(false)
                    binding.etName.doOnTextChanged { text, _, _, _ ->
                        isNameSet.value = !text.isNullOrEmpty()
                    }

                    // listen to tag text and color changes to enable/disable the positive button
                    lifecycleScope.launch {
                        combine(tagColor, isNameSet) { color, isNameSet ->
                            Log.d("CreateTagDialog", "color: $color, isNameSet: $isNameSet")
                            color.isNotEmpty() && isNameSet
                        }.collectLatest {
                            positiveButton.isEnabled = it
                        }
                    }
                }
            }
            .show()
    }

    private class ColorPickerAdapter(private val onItemClicked: (String) -> Unit) :
        RecyclerView.Adapter<ColorPickerAdapter.ItemColorPreviewViewHolder>() {

        init {
            ItemColorPreviewViewHolder.prevSelectedPos = -1
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ItemColorPreviewViewHolder {
            return ItemColorPreviewViewHolder(
                ItemColorPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ) { position ->
                if (ItemColorPreviewViewHolder.prevSelectedPos != position) {
                    // "unselect" previous color item
                    notifyItemChanged(ItemColorPreviewViewHolder.prevSelectedPos)
                    ItemColorPreviewViewHolder.prevSelectedPos = position
                    // "select" current color item
                    notifyItemChanged(position)
                    onItemClicked(defaultTagColors[position])
                }
            }
        }

        override fun onBindViewHolder(holder: ItemColorPreviewViewHolder, position: Int) {
            holder.bind(defaultTagColors[position])
        }

        override fun getItemCount(): Int = defaultTagColors.size

        class ItemColorPreviewViewHolder(
            private val binding: ItemColorPreviewBinding,
            private val clickAtPosition: (Int) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            companion object {
                var prevSelectedPos = -1
            }

            init {
                binding.cardColor.setOnClickListener {
                    if (prevSelectedPos != adapterPosition) {
                        clickAtPosition(adapterPosition)
                    }
                }
            }

            fun bind(hexColorValue: String) {
                binding.apply {
                    cardColor.isSelected = adapterPosition == prevSelectedPos
                    if (cardColor.isSelected) {
                        cardColor.strokeColor =
                            getColor(itemView.context, R.color.md_theme_light_primary)
                    } else {
                        cardColor.strokeColor =
                            getColor(itemView.context, R.color.md_theme_light_outlineVariant)
                    }

                    cardColor.setCardBackgroundColor(Color.parseColor(hexColorValue))
                }
            }
        }
    }
}