package com.example.kanbun.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
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