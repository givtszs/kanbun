package com.example.kanbun.presentation

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
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
    protected abstract fun setUpListeners()

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

    /**
     * Sets up the action bar with the provided [toolbar] and a custom [navigation action][navigate].
     * @param toolbar custom toolbar to be set up as the action bar.
     * @param navigate callback function executed when either the `Navigate up` or `Back` button is pressed.
     */
    protected open fun setUpActionBar(toolbar: MaterialToolbar, navigate: () -> Unit) {
        setUpActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            navigate()
        }
        addOnBackPressedAction(navigate)
    }

    protected fun showToast(message: String, context: Context = requireContext(), duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }

    protected fun setStatusBarColor(color: Int) {
        requireActivity().window.apply {
            statusBarColor = color
        }
    }
}