package com.example.kanbun.presentation.registration

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.example.kanbun.R
import com.example.kanbun.presentation.BaseFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
/**
 * Parent class for authentication-related fragments such as `Sign in` and `Sign up` fragments
 */
abstract class AuthFragment : BaseFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    /**
     * Clears the focus of the text fields, e.g. when the virtual keyboard is hidden.
     */
    protected abstract fun clearTextFieldFocus(view: View)

    /**
     * Sets up a [textField]'s `on text changed` callback with the provided [editText] and [viewModel].
     * @param textField text field to be set up
     * @param editText the inner [TextInputEditText] layout of the text field
     * @param viewModel [AuthViewModel] instance
     */
    protected fun setUpTextField(textField: TextInputLayout, editText: TextInputEditText, viewModel: AuthViewModel) {
        editText.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) {
                textField.apply {
                    isErrorEnabled = false
                    viewModel.resetTextFieldError(id)
                }
            }
        }
    }

    /**
     * Callback to check email verification status for a [FirebaseUser].
     */
    protected val checkEmailVerificationCallback: (FirebaseUser) -> Unit = { user ->
        Log.d("AuthResult", "email: ${user.email}, name: ${user.displayName}, photo: ${user.photoUrl}")
        if (!user.isEmailVerified) {
            navController.navigate(R.id.emailVerificationFragment)
        } else {
            showToast("Email is already verified")
            navController.navigate(R.id.userBoardsFragment)
        }
    }

    protected fun hideKeyboard(view: View) {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(view.applicationWindowToken, 0)
        }
    }

    /**
     * Callback to handle the Google authentication process.
     * @param idToken see [GoogleSignInAccount.getIdToken]
     */
    abstract fun googleAuthCallback(idToken: String?)

    /**
     * Activity result launcher to handler the result of Google Sign-In flow.
     */
    protected val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                GoogleSignIn.getSignedInAccountFromIntent(data).result.also { account ->
                    googleAuthCallback(account.idToken)
                }
            } else {
                showToast("Couldn't complete a request")
            }
        }
}