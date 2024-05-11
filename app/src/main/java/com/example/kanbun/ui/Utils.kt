package com.example.kanbun.ui

import android.content.Context
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import com.example.kanbun.R
import com.example.kanbun.databinding.AlertDialogCreateItemBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun buildDeleteConfirmationDialog(
    context: Context,
    @StringRes titleRes: Int,
    onConfirm: () -> Unit
): AlertDialog {
    return MaterialAlertDialogBuilder(context)
        .setTitle(titleRes)
        .setPositiveButton("Delete") { _, _ ->
            onConfirm()
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        .create()
}

fun buildCreateItemDialog(
    context: Context,
    @StringRes item: Int,
    onPositiveButtonClicked: (String) -> Unit
) {
    val itemStr = context.resources.getString(item)
    val title = context.resources.getString(R.string.create_item, itemStr)
    val dialogBinding = AlertDialogCreateItemBinding.inflate(
        LayoutInflater.from(context),
        null,
        false
    ).apply {
        etName.setHint(item)
        tvHint.text = context.resources.getString(R.string.hint_item, itemStr)
    }


    MaterialAlertDialogBuilder(context, R.style.MaterialDialog)
        .setTitle(title)
        .setView(dialogBinding.root)
        .setPositiveButton(R.string.positive_button_create) { _, _ ->
            onPositiveButtonClicked(dialogBinding.etName.text.toString())
        }
        .setNegativeButton(R.string.negative_button_cancel) { dialog, _ ->
            dialog.cancel()
        }
        .create()
        .apply {
            setOnShowListener {
                val posButton =
                    getButton(android.app.AlertDialog.BUTTON_POSITIVE).apply { isEnabled = false }

                dialogBinding.etName.doOnTextChanged { text, _, _, _ ->
                    posButton.isEnabled = text?.trim().isNullOrEmpty() == false
                }
            }
        }
        .show()
}