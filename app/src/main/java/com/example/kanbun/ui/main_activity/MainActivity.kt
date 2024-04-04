package com.example.kanbun.ui.main_activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.example.kanbun.R
import com.example.kanbun.common.DrawerItem
import com.example.kanbun.databinding.ActivityMainBinding
import com.google.firebase.FirebaseError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        // TODO: Update on user re sign in
        private var firebaseAuth = Firebase.auth
        val firebaseUser get() = firebaseAuth.currentUser
        fun signOut() {
            firebaseAuth.signOut()
        }

    }

    private var _binding: ActivityMainBinding? = null
    val activityMainBinding: ActivityMainBinding get() = _binding!!
    private lateinit var navController: NavController
    lateinit var appBarConfiguration: AppBarConfiguration

    var userWorkspacesAdapter: DrawerAdapter? = null
    var sharedWorkspacesAdapter: DrawerAdapter? = null
    var isSharedBoardsSelected: Boolean? = null
        set(value) {
            field = value
            activityMainBinding.drawerContent.sharedBoards.cardBackground.isSelected = field == true
            activityMainBinding.drawerContent.sharedBoards.tvName.isSelected = field == true
            activityMainBinding.drawerContent.sharedBoards.ivLeadingIcon.isSelected = field == true
        }

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


        // destinations that should hide navigation bar
        val navBarlessDestinations = setOf(
            R.id.registrationPromptFragment,
            R.id.signUpFragment,
            R.id.signInFragment,
            R.id.emailVerificationFragment,
            R.id.workspaceSettingsFragment,
            R.id.boardFragment,
            R.id.createTaskFragment,
            R.id.taskDetailsFragment,
            R.id.boardSettingsFragment
        )

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            val hideNavBar = navBarlessDestinations.contains(destination.id)

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
        activityMainBinding.drawerContent.headerLayout.apply {
            ivProfilePicture.setImageResource(R.drawable.ic_launcher_background)
            // TODO: Check why do I even have this
            tvName.text = "Awesome Name"
            tvEmail.text = "awesome@email.com"
        }

        activityMainBinding.drawerContent.apply {
            userWorkspacesAdapter = DrawerAdapter()
            rvUserWorkspaces.adapter = userWorkspacesAdapter

            sharedWorkspacesAdapter = DrawerAdapter()
            rvSharedWorkspaces.adapter = sharedWorkspacesAdapter

            userWorkspacesHeadline.tvHeadline.text = resources.getString(R.string.user_workspaces_headline)
            userWorkspacesHeadline.btnRecyclerViewToggle.setOnClickListener {
                rvUserWorkspaces.isVisible = !rvUserWorkspaces.isVisible
            }
            sharedWorkspacesHeadline.tvHeadline.text = resources.getString(R.string.shared_workspaces_headline)
            sharedWorkspacesHeadline.btnRecyclerViewToggle.setOnClickListener {
                rvSharedWorkspaces.isVisible = !rvSharedWorkspaces.isVisible
            }

            sharedBoards.card.setOnClickListener {
                DrawerAdapter.onItemClickCallback(DrawerItem.SHARED_BOARDS)
            }
            sharedBoards.tvName.text = resources.getString(R.string.shared_boards_headline)
            sharedBoards.ivLeadingIcon.setImageResource(R.drawable.ic_kanban_board_selector)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        userWorkspacesAdapter = null
    }
}