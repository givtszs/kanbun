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
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.DrawerItem
import com.example.kanbun.databinding.ActivityMainBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint

interface DrawerListeners {
    fun onSignOutClick()
    fun onSettingsClick()
    fun onCreateWorkspaceClick()
}

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

    var drawerListeners: DrawerListeners? = null

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
            R.id.boardSettingsFragment,
            R.id.settingsFragment,
            R.id.editProfileFragment,
            R.id.editTaskFragment
        )

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            activityMainBinding.navBar.isVisible = !navBarlessDestinations.contains(destination.id)

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
        activityMainBinding.drawerContent.apply {
            headerLayout.btnSignOut.setOnClickListener {
                drawerListeners?.onSignOutClick()
            }

            headerLayout.btnSettings.setOnClickListener {
                drawerListeners?.onSettingsClick()
            }

            btnCreateWorkspace.setOnClickListener {
                drawerListeners?.onCreateWorkspaceClick()
            }

            userWorkspacesAdapter = DrawerAdapter()
            rvUserWorkspaces.adapter = userWorkspacesAdapter

            sharedWorkspacesAdapter = DrawerAdapter()
            rvSharedWorkspaces.adapter = sharedWorkspacesAdapter

            userWorkspacesHeadline.tvHeadline.text = resources.getString(R.string.user_workspaces_headline)
            userWorkspacesHeadline.btnRecyclerViewToggle.setOnClickListener {
                toggleRecyclerView(rvUserWorkspaces, userWorkspacesHeadline.btnRecyclerViewToggle)
            }
            sharedWorkspacesHeadline.tvHeadline.text = resources.getString(R.string.shared_workspaces_headline)
            sharedWorkspacesHeadline.btnRecyclerViewToggle.setOnClickListener {
                toggleRecyclerView(rvSharedWorkspaces, sharedWorkspacesHeadline.btnRecyclerViewToggle)
            }

            sharedBoards.card.setOnClickListener {
                DrawerAdapter.onItemClickCallback?.invoke(DrawerItem.SHARED_BOARDS)
            }
            sharedBoards.tvName.text = resources.getString(R.string.shared_boards_headline)
            sharedBoards.ivLeadingIcon.setImageResource(R.drawable.ic_kanban_board_selector)
        }
    }

    private fun toggleRecyclerView(recyclerView: RecyclerView, view: View) {
        if (recyclerView.isVisible) {
            view.animate().setDuration(200).rotation(90.0f)
        } else {
            view.animate().setDuration(200).rotation(0.0f)
        }
        recyclerView.isVisible = !recyclerView.isVisible
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