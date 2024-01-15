package com.example.kanbun.presentation

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.kanbun.R
import com.example.kanbun.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = _binding!!
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setUpNavController()

        binding.navBar.setupWithNavController(navController)
    }

    private fun setUpNavController() {
        val navHostFragment = supportFragmentManager.findFragmentById(
            androidx.navigation.fragment.R.id.nav_host_fragment_container
        ) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            val hideNavBar = destination.id == R.id.registrationPromptFragment
                    || destination.id == R.id.signUpFragment
                    || destination.id == R.id.signInFragment
                    || destination.id == R.id.emailVerificationFragment

            if (hideNavBar) {
                binding.navBar.visibility = View.GONE
            } else {
                binding.navBar.visibility = View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}