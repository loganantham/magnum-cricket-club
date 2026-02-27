package com.magnum.cricketclub.ui.teamprofile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.UserProfileRepository
import com.magnum.cricketclub.data.remote.FirestoreRepository
import com.magnum.cricketclub.ui.BaseActivity
import kotlinx.coroutines.launch

class TeamProfileActivity : BaseActivity() {
    private lateinit var teamMembersRecyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var teamMemberAdapter: TeamMemberAdapter
    private lateinit var userProfileRepository: UserProfileRepository
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_profile)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.team_profile_screen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        userProfileRepository = UserProfileRepository(application)
        
        teamMembersRecyclerView = findViewById(R.id.teamMembersRecyclerView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)
        
        teamMemberAdapter = TeamMemberAdapter()
        teamMembersRecyclerView.layoutManager = LinearLayoutManager(this)
        teamMembersRecyclerView.adapter = teamMemberAdapter
        
        loadTeamMembers()
        
        // Setup bottom navigation
        setupBottomNavigation()
    }
    
    private fun loadTeamMembers() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TeamProfileActivity", "Loading team members from Firebase...")
                
                // Try to load from Firebase first
                val firebaseProfiles = try {
                    firestoreRepository.downloadAllUserProfiles()
                } catch (e: Exception) {
                    android.util.Log.e("TeamProfileActivity", "Error loading from Firebase", e)
                    emptyList()
                }
                
                android.util.Log.d("TeamProfileActivity", "Found ${firebaseProfiles.size} profiles from Firebase")
                
                // Also get local profiles
                val localProfiles = try {
                    userProfileRepository.getAllUserProfiles()
                } catch (e: Exception) {
                    android.util.Log.e("TeamProfileActivity", "Error loading from local DB", e)
                    emptyList()
                }
                
                android.util.Log.d("TeamProfileActivity", "Found ${localProfiles.size} profiles from local DB")
                
                // Combine both lists, prioritizing Firebase data
                val allProfiles = (firebaseProfiles + localProfiles).distinctBy { it.email }
                
                android.util.Log.d("TeamProfileActivity", "Total unique profiles: ${allProfiles.size}")
                
                if (allProfiles.isEmpty()) {
                    emptyStateTextView.visibility = View.VISIBLE
                    teamMembersRecyclerView.visibility = View.GONE
                    android.util.Log.d("TeamProfileActivity", "No team members found")
                } else {
                    emptyStateTextView.visibility = View.GONE
                    teamMembersRecyclerView.visibility = View.VISIBLE
                    teamMemberAdapter.submitList(allProfiles)
                    android.util.Log.d("TeamProfileActivity", "Displaying ${allProfiles.size} team members")
                }
            } catch (e: Exception) {
                android.util.Log.e("TeamProfileActivity", "Error loading team members", e)
                Toast.makeText(this@TeamProfileActivity, "Error loading team members: ${e.message}", Toast.LENGTH_SHORT).show()
                emptyStateTextView.visibility = View.VISIBLE
                teamMembersRecyclerView.visibility = View.GONE
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
