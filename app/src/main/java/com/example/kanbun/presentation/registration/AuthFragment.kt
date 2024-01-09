package com.example.kanbun.presentation.registration

import android.view.View
import com.example.kanbun.presentation.BaseFragment
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class AuthFragment : BaseFragment() {
    protected abstract fun clearTextFieldFocus(view: View)
    protected abstract fun showTextFieldError(input: TextInputLayout, message: String)
}