package com.example.kanbun.presentation

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.example.kanbun.R
import com.example.kanbun.databinding.ActivityMainBinding
import com.example.kanbun.presentation.root.user_boards.DrawerAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = _binding!!
    private lateinit var navController: NavController
    lateinit var appBarConfiguration: AppBarConfiguration

    private var drawerAdapter: DrawerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpNavigation()
        setUpNavView()
    }

    private fun setUpNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(
            androidx.navigation.fragment.R.id.nav_host_fragment_container
        ) as NavHostFragment

        // set up nav controller
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph, binding.root)

        val topLevelDestinations = setOf(
            R.id.registrationPromptFragment,
            R.id.signUpFragment,
            R.id.signInFragment,
            R.id.emailVerificationFragment
        )

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            val hideNavBar = topLevelDestinations.contains(destination.id)

            if (hideNavBar) {
                binding.navBar.visibility = View.GONE
            } else {
                binding.navBar.visibility = View.VISIBLE
            }

            val hideNavDrawer = destination.id != R.id.userBoardsFragment
            if (hideNavDrawer) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
        }

        // set up nav bar
        binding.navBar.setupWithNavController(navController)

        // set up nav view
        binding.navView.setupWithNavController(navController)
    }

    private fun setUpNavView() {
        // set up header layout
        binding.headerLayout.apply {
            ivProfilePicture.setImageResource(R.drawable.ic_launcher_background)
            tvName.text = "Awesome Name"
            tvEmail.text = "awesome@email.com"
        }

        drawerAdapter = DrawerAdapter(this) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.navRecyclerView.adapter = drawerAdapter

        binding.createWorkspace.setOnClickListener {
            val lastNum = drawerAdapter?.workspaces?.last()?.name?.substringAfter("Workspace ")?.toInt() ?: -1000
            drawerAdapter?.addData(DrawerAdapter.WorkspaceModel(name = "Workspace ${(lastNum + 1)}"))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        drawerAdapter = null
    }
}