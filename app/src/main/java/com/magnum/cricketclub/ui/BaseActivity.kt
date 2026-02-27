package com.magnum.cricketclub.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.magnum.cricketclub.R
import com.magnum.cricketclub.ui.me.MeActivity
import com.magnum.cricketclub.ui.teamprofile.TeamProfileActivity

abstract class BaseActivity : AppCompatActivity() {
    protected var bottomNavigation: BottomNavigationView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    
    protected fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        val nav = bottomNavigation ?: return
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_me -> {
                    if (this !is MeActivity) {
                        startActivity(Intent(this, MeActivity::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                R.id.nav_expenses -> {
                    if (this !is com.magnum.cricketclub.ui.MainActivity) {
                        startActivity(Intent(this, com.magnum.cricketclub.ui.MainActivity::class.java))
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
            is MeActivity -> bottomNavigation!!.selectedItemId = R.id.nav_me
            is com.magnum.cricketclub.ui.MainActivity -> bottomNavigation!!.selectedItemId = R.id.nav_expenses
            is TeamProfileActivity -> bottomNavigation!!.selectedItemId = R.id.nav_team_profile
        }
    }
}
