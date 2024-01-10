package com.example.kanbun.presentation.registration

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.kanbun.R
import com.example.kanbun.presentation.BaseFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class AuthFragment : BaseFragment() {
    protected abstract fun clearTextFieldFocus(view: View)

    abstract fun googleAuthCallback(idToken: String?)

    protected val authSuccessCallback: (FirebaseUser) -> Unit = { user ->
        if (!user.isEmailVerified) {
            navController.navigate(R.id.emailVerificationFragment)
        } else {
            // navigate to home screen
            showToast("User is already verified")
        }
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        // TODO("Delete when sign out button is implemented")
        Firebase.auth.signOut()
    }

    protected val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            GoogleSignIn.getSignedInAccountFromIntent(data).result.also {
                googleAuthCallback(it.idToken)
            }
        } else {
            showToast("Couldn't complete a request")
        }
    }
}