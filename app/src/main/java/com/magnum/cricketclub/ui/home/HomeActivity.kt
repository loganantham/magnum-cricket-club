package com.magnum.cricketclub.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.UserProfileRepository
import com.magnum.cricketclub.ui.BaseActivity
import com.magnum.cricketclub.utils.UpcomingMatchStore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HomeActivity : BaseActivity() {

    private lateinit var upcomingMatchCard: View
    private lateinit var noUpcomingMatchTextView: TextView
    private lateinit var matchTeamsTextView: TextView
    private lateinit var matchDateTextView: TextView
    private lateinit var matchGroundTextView: TextView
    private lateinit var matchLocationTextView: TextView
    private lateinit var addMatchButton: MaterialButton
    private lateinit var userProfileRepository: UserProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.home)

        userProfileRepository = UserProfileRepository(application)

        upcomingMatchCard = findViewById(R.id.upcomingMatchCard)
        noUpcomingMatchTextView = findViewById(R.id.noUpcomingMatchTextView)
        matchTeamsTextView = findViewById(R.id.matchTeamsTextView)
        matchDateTextView = findViewById(R.id.matchDateTextView)
        matchGroundTextView = findViewById(R.id.matchGroundTextView)
        matchLocationTextView = findViewById(R.id.matchLocationTextView)
        addMatchButton = findViewById(R.id.addMatchButton)

        addMatchButton.setOnClickListener {
            startActivity(Intent(this, com.magnum.cricketclub.ui.teamprofile.TeamProfileActivity::class.java))
        }
        
        setupBottomNavigation()

        loadUpcomingMatch()
    }

    override fun onResume() {
        super.onResume()
        loadUpcomingMatch()
        updateBottomNavigationSelection()
    }

    private fun loadUpcomingMatch() {
        lifecycleScope.launch {
            val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
            val profile = userProfileRepository.getUserProfileSync(email)
            val canManageMatches = profile?.canManageMatches() == true

            val match = UpcomingMatchStore.load(this@HomeActivity)
            if (match != null) {
                noUpcomingMatchTextView.visibility = View.GONE
                matchTeamsTextView.visibility = View.VISIBLE
                matchDateTextView.visibility = View.VISIBLE
                matchGroundTextView.visibility = View.VISIBLE
                matchLocationTextView.visibility = View.VISIBLE
                
                val formatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                matchTeamsTextView.text = "${match.team1} vs ${match.team2}"
                matchDateTextView.text = formatter.format(Date(match.dateUtcMillis))
                matchGroundTextView.text = match.groundName
                matchLocationTextView.text = match.groundLocation

                upcomingMatchCard.setOnClickListener {
                    startActivity(Intent(this@HomeActivity, com.magnum.cricketclub.ui.teamprofile.TeamProfileActivity::class.java))
                }
                
                addMatchButton.text = if (canManageMatches) "Manage Match" else "View Details"
            } else {
                noUpcomingMatchTextView.visibility = View.VISIBLE
                matchTeamsTextView.visibility = View.GONE
                matchDateTextView.visibility = View.GONE
                matchGroundTextView.visibility = View.GONE
                matchLocationTextView.visibility = View.GONE
                
                addMatchButton.visibility = if (canManageMatches) View.VISIBLE else View.GONE
                addMatchButton.text = "Schedule a Match"
            }
        }
    }
}
