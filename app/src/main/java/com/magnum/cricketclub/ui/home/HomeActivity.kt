package com.magnum.cricketclub.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.UserProfileRepository
import com.magnum.cricketclub.data.remote.FirestoreRepository
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
    private lateinit var availabilityLayout: View
    private lateinit var availabilityRadioGroup: RadioGroup
    private lateinit var availableYes: RadioButton
    private lateinit var availableNo: RadioButton
    private lateinit var reasonInputLayout: View
    private lateinit var reasonEditText: EditText
    private lateinit var saveAvailabilityButton: MaterialButton
    private lateinit var savedReasonTextView: TextView
    private lateinit var addMatchButton: MaterialButton
    private lateinit var userProfileRepository: UserProfileRepository
    private val firestoreRepository = FirestoreRepository()

    private var currentMatchDateUtcMillis: Long = 0
    private var isUpdatingUI = false

    private val checkedChangeListener = RadioGroup.OnCheckedChangeListener { _, checkedId ->
        if (isUpdatingUI) return@OnCheckedChangeListener
        
        if (checkedId == R.id.availableNo) {
            reasonInputLayout.visibility = View.VISIBLE
            saveAvailabilityButton.visibility = View.VISIBLE
        } else if (checkedId == R.id.availableYes) {
            reasonInputLayout.visibility = View.GONE
            saveAvailabilityButton.visibility = View.GONE
            showYesConfirmationDialog()
        }
    }

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
        availabilityLayout = findViewById(R.id.availabilityLayout)
        availabilityRadioGroup = findViewById(R.id.availabilityRadioGroup)
        availableYes = findViewById(R.id.availableYes)
        availableNo = findViewById(R.id.availableNo)
        reasonInputLayout = findViewById(R.id.reasonInputLayout)
        reasonEditText = findViewById(R.id.reasonEditText)
        saveAvailabilityButton = findViewById(R.id.saveAvailabilityButton)
        savedReasonTextView = findViewById(R.id.savedReasonTextView)
        addMatchButton = findViewById(R.id.addMatchButton)

        addMatchButton.setOnClickListener {
            startActivity(Intent(this, com.magnum.cricketclub.ui.teamprofile.TeamProfileActivity::class.java))
        }

        availabilityRadioGroup.setOnCheckedChangeListener(checkedChangeListener)

        saveAvailabilityButton.setOnClickListener {
            val isAvailable = availabilityRadioGroup.checkedRadioButtonId == R.id.availableYes
            val reason = if (!isAvailable) reasonEditText.text.toString().trim() else null
            
            if (!isAvailable && reason.isNullOrBlank()) {
                reasonEditText.error = "Please provide a reason"
                return@setOnClickListener
            }
            
            saveAvailability(isAvailable, reason)
        }
        
        setupBottomNavigation()

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
        updateBottomNavigationSelection()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
            val profile = userProfileRepository.getUserProfileSync(email)
            val canManageMatches = profile?.canManageMatches() == true

            // Try to sync from Firestore first
            try {
                firestoreRepository.downloadUpcomingMatch()?.let {
                    UpcomingMatchStore.save(this@HomeActivity, it)
                }
            } catch (e: Exception) {
                // Ignore and use local data if sync fails
            }

            val match = UpcomingMatchStore.load(this@HomeActivity)
            if (match != null) {
                currentMatchDateUtcMillis = match.dateUtcMillis
                noUpcomingMatchTextView.visibility = View.GONE
                matchTeamsTextView.visibility = View.VISIBLE
                matchDateTextView.visibility = View.VISIBLE
                matchGroundTextView.visibility = View.VISIBLE
                matchLocationTextView.visibility = View.VISIBLE
                availabilityLayout.visibility = View.VISIBLE
                
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
                addMatchButton.visibility = View.VISIBLE

                // Load user's availability
                loadAvailability(match.dateUtcMillis)
            } else {
                currentMatchDateUtcMillis = 0
                noUpcomingMatchTextView.visibility = View.VISIBLE
                matchTeamsTextView.visibility = View.GONE
                matchDateTextView.visibility = View.GONE
                matchGroundTextView.visibility = View.GONE
                matchLocationTextView.visibility = View.GONE
                availabilityLayout.visibility = View.GONE
                
                addMatchButton.visibility = if (canManageMatches) View.VISIBLE else View.GONE
                addMatchButton.text = "Schedule a Match"
            }
        }
    }

    private fun loadAvailability(matchDate: Long) {
        lifecycleScope.launch {
            try {
                val availability = firestoreRepository.downloadMatchAvailability(matchDate)
                isUpdatingUI = true
                if (availability != null) {
                    // Selection already exists, disable the UI
                    disableAvailabilityUI(availability.available, availability.reason)
                } else {
                    // No selection yet, enable UI
                    enableAvailabilityUI()
                }
                isUpdatingUI = false
            } catch (e: Exception) {
                isUpdatingUI = false
                enableAvailabilityUI()
            }
        }
    }

    private fun enableAvailabilityUI() {
        availabilityRadioGroup.clearCheck()
        availableYes.isEnabled = true
        availableNo.isEnabled = true
        reasonInputLayout.visibility = View.GONE
        saveAvailabilityButton.visibility = View.GONE
        savedReasonTextView.visibility = View.GONE
    }

    private fun disableAvailabilityUI(isAvailable: Boolean, reason: String?) {
        availableYes.isChecked = isAvailable
        availableNo.isChecked = !isAvailable
        availableYes.isEnabled = false
        availableNo.isEnabled = false
        
        reasonInputLayout.visibility = View.GONE
        saveAvailabilityButton.visibility = View.GONE
        
        if (!isAvailable && !reason.isNullOrBlank()) {
            savedReasonTextView.text = "Reason: $reason"
            savedReasonTextView.visibility = View.VISIBLE
        } else {
            savedReasonTextView.visibility = View.GONE
        }
    }

    private fun showYesConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Availability")
            .setMessage("Are you sure you are available for this match?")
            .setPositiveButton("OK") { _, _ ->
                saveAvailability(true, null)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                isUpdatingUI = true
                availabilityRadioGroup.clearCheck()
                isUpdatingUI = false
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun saveAvailability(isAvailable: Boolean, reason: String?) {
        lifecycleScope.launch {
            try {
                firestoreRepository.uploadMatchAvailability(isAvailable, currentMatchDateUtcMillis, reason)
                Toast.makeText(this@HomeActivity, "Availability confirmed", Toast.LENGTH_SHORT).show()
                disableAvailabilityUI(isAvailable, reason)
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Failed to confirm availability", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
