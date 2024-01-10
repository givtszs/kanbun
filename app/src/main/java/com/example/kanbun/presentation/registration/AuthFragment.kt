package com.example.kanbun.presentation.registration

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.kanbun.R
import com.example.kanbun.presentation.BaseFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class AuthFragment : BaseFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    protected abstract fun clearTextFieldFocus(view: View)

    protected fun showTextFieldError(input: TextInputLayout, message: String) {
        input.apply {
            error = message
            isErrorEnabled = true
        }
    }

    protected val checkEmailVerificationCallback: (FirebaseUser) -> Unit = { user ->
        Log.d("AuthResult", "email: ${user.email}, name: ${user.displayName}, photo: ${user.photoUrl}")
        if (!user.isEmailVerified) {
            navController.navigate(R.id.emailVerificationFragment)
        } else {
            // navigate to home screen
            showToast("User is already verified")
        }
    }

    protected fun hideKeyboard(view: View) {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(view.applicationWindowToken, 0)
        }
    }

    abstract fun googleAuthCallback(idToken: String?)

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