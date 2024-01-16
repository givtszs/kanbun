package com.example.kanbun.presentation.main_activity

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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    val activityMainBinding: ActivityMainBinding get() = _binding!!
    private lateinit var navController: NavController
    lateinit var appBarConfiguration: AppBarConfiguration

    private var drawerAdapter: DrawerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        setUpNavigation()
        setUpNavView()
    }

    private fun setUpNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(
            androidx.navigation.fragment.R.id.nav_host_fragment_container
        ) as NavHostFragment

        // set up nav controller
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph, activityMainBinding.root)

        val topLevelDestinations = setOf(
            R.id.registrationPromptFragment,
            R.id.signUpFragment,
            R.id.signInFragment,
            R.id.emailVerificationFragment
        )

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            val hideNavBar = topLevelDestinations.contains(destination.id)

            if (hideNavBar) {
                activityMainBinding.navBar.visibility = View.GONE
            } else {
                activityMainBinding.navBar.visibility = View.VISIBLE
            }

            val hideNavDrawer = destination.id != R.id.userBoardsFragment
            if (hideNavDrawer) {
                activityMainBinding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                activityMainBinding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
        }

        // set up nav bar
        activityMainBinding.navBar.setupWithNavController(navController)

        // set up nav view
        activityMainBinding.navView.setupWithNavController(navController)
    }

    private fun setUpNavView() {
        // set up header layout
        activityMainBinding.headerLayout.apply {
            ivProfilePicture.setImageResource(R.drawable.ic_launcher_background)
            tvName.text = "Awesome Name"
            tvEmail.text = "awesome@email.com"
        }

        drawerAdapter = DrawerAdapter(this) {
            activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        activityMainBinding.navRecyclerView.adapter = drawerAdapter

        activityMainBinding.createWorkspace.setOnClickListener {
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