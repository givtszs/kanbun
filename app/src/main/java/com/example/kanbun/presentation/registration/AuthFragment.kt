package com.example.kanbun.presentation.registration

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.example.kanbun.R
import com.example.kanbun.presentation.BaseFragment
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class AuthFragment : BaseFragment() {
    protected abstract fun clearTextFieldFocus(view: View)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    protected fun hideKeyboard(view: View) {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(view.applicationWindowToken, 0)
        }
    }

    protected fun showTextFieldError(input: TextInputLayout, message: String) {
        input.apply {
            error = message
            isErrorEnabled = true
        }
    }
}