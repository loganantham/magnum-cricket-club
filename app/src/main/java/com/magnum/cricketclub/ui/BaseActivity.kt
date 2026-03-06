package com.magnum.cricketclub.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.UserProfile
import com.magnum.cricketclub.data.UserProfileRepository
import com.magnum.cricketclub.data.remote.FirestoreRepository
import com.magnum.cricketclub.ui.config.ConfigActivity
import com.magnum.cricketclub.ui.home.HomeActivity
import com.magnum.cricketclub.ui.me.MeActivity
import com.magnum.cricketclub.ui.teamprofile.TeamProfileActivity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {
    protected var bottomNavigation: BottomNavigationView? = null
    private val firestoreRepository = FirestoreRepository()
    private lateinit var userProfileRepository: UserProfileRepository
    private var currentUserProfile: UserProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userProfileRepository = UserProfileRepository(application)
        
        lifecycleScope.launch {
            val email = firestoreRepository.getCurrentUserEmail() ?: return@launch
            currentUserProfile = userProfileRepository.getUserProfile(email).firstOrNull()
            invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        
        // Only show settings icon to Finance Maintenance (Admins)
        val settingsItem = menu.findInternal(R.id.menu_settings)
        settingsItem?.isVisible = currentUserProfile?.canManageFinance() == true
        
        return true
    }

    private fun Menu.findInternal(id: Int): MenuItem? {
        for (i in 0 until size()) {
            val item = getItem(i)
            if (item.itemId == id) return item
        }
        return null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_home -> {
                if (this !is HomeActivity) {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                }
                true
            }
            R.id.menu_notifications -> {
                Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_settings -> {
                startActivity(Intent(this, ConfigActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    protected fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        val nav = bottomNavigation ?: return
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is HomeActivity) {
                        startActivity(Intent(this, HomeActivity::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                R.id.nav_me -> {
                    if (this !is MeActivity) {
                        startActivity(Intent(this, MeActivity::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                R.id.nav_expenses -> {
                    if (this !is MainActivity) {
                        startActivity(Intent(this, MainActivity::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                R.id.nav_team_profile -> {
                    if (this !is TeamProfileActivity) {
                        startActivity(Intent(this, TeamProfileActivity::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                else -> false
            }
        }
        
        // Set the selected item based on current activity
        updateBottomNavigationSelection()
    }
    
    protected fun updateBottomNavigationSelection() {
        bottomNavigation ?: return
        when (this) {
            is HomeActivity -> bottomNavigation!!.selectedItemId = R.id.nav_home
            is MeActivity -> bottomNavigation!!.selectedItemId = R.id.nav_me
            is MainActivity -> bottomNavigation!!.selectedItemId = R.id.nav_expenses
            is TeamProfileActivity -> bottomNavigation!!.selectedItemId = R.id.nav_team_profile
        }
    }
}
