package com.example.kanbun.presentation

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
abstract class BaseFragment : Fragment() {
    lateinit var navController: NavController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()
        setUpListeners()
    }

    protected abstract fun setUpListeners()

    protected fun addOnBackPressedAction(navigate: () -> Unit) {
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigate()
            }
        })
    }

    protected fun setUpActionBar(toolbar: MaterialToolbar) {
        (requireActivity() as MainActivity).apply {
            setSupportActionBar(toolbar)
            setupActionBarWithNavController(navController)
        }
    }

    protected fun setUpActionBar(toolbar: MaterialToolbar, navigate: () -> Unit) {
        setUpActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            navigate()
        }
        addOnBackPressedAction(navigate)
    }

    protected fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), message, duration).show()
    }

    protected fun setStatusBarColor(color: Int) {
        requireActivity().window.apply {
            statusBarColor = color
        }
    }
}