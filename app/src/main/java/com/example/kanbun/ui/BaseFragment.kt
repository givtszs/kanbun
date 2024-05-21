package com.example.kanbun.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.kanbun.ui.main_activity.MainActivity
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
/**
 * Base class for fragments in the application.
 * Provides common functionality such as navigation, action bar set up, and UI-related utilities.
 */
abstract class BaseFragment : Fragment() {
    /**
     * [Navigation controller][NavController] for the current fragment
      */
    lateinit var navController: NavController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()
        setUpListeners()
    }

    /**
     * Sets up listeners for UI interactions.
     *
     * View binding may also be set in this method.
     */
    protected abstract fun setUpListeners() // TODO: Rename to `setUpViews`

    /**
     * Adds a custom [navigation action][navigate] to be executed for the current fragment when
     * the back button is pressed.
     * @param navigate callback function executed when the back button is pressed
     */
    protected fun addOnBackPressedAction(navigate: () -> Unit) {
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigate()
            }
        })
    }

    protected open fun setUpActionBar(toolbar: MaterialToolbar) {
        (requireActivity() as MainActivity).apply {
            setSupportActionBar(toolbar)
            setupActionBarWithNavController(navController)
        }
    }

    protected open fun setUpActionBar(toolbar: MaterialToolbar, title: String) {
        setUpActionBar(toolbar)
        toolbar.title = title
    }

    protected fun showToast(message: String, context: Context = requireContext(), duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }

    protected fun setStatusBarColor(color: Int) {
        requireActivity().window.apply {
            statusBarColor = color
        }
    }

    protected fun hideKeyboard(view: View) {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(view.applicationWindowToken, 0)
        }
    }
}