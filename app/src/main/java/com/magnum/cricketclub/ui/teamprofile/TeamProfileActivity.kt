package com.magnum.cricketclub.ui.teamprofile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.UpcomingMatch
import com.magnum.cricketclub.data.UserProfile
import com.magnum.cricketclub.data.UserProfileRepository
import com.magnum.cricketclub.data.remote.FirestoreMatchAvailability
import com.magnum.cricketclub.data.remote.FirestoreRepository
import com.magnum.cricketclub.ui.BaseActivity
import com.magnum.cricketclub.utils.UpcomingMatchStore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TeamProfileActivity : BaseActivity() {
    private lateinit var upcomingMatchHeader: LinearLayout
    private lateinit var upcomingMatchContent: LinearLayout
    private lateinit var upcomingMatchChevron: ImageView
    private lateinit var upcomingMatchFormCard: View

    private lateinit var matchDetailsHeader: LinearLayout
    private lateinit var matchDetailsContent: LinearLayout
    private lateinit var matchDetailsChevron: ImageView

    private lateinit var playerAvailabilityHeader: LinearLayout
    private lateinit var playerAvailabilityContent: LinearLayout
    private lateinit var playerAvailabilityChevron: ImageView
    private lateinit var availabilityRecyclerView: RecyclerView
    private lateinit var emptyAvailabilityTextView: TextView
    private lateinit var playerAvailabilityAdapter: PlayerAvailabilityAdapter
    private lateinit var availabilityFilterGroup: MaterialButtonToggleGroup

    private lateinit var teamMembersHeader: LinearLayout
    private lateinit var teamMembersContent: LinearLayout
    private lateinit var teamMembersChevron: ImageView

    private lateinit var teamMembersRecyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var teamMemberAdapter: TeamMemberAdapter
    private lateinit var userProfileRepository: UserProfileRepository
    private val firestoreRepository = FirestoreRepository()

    private lateinit var matchDateContainer: LinearLayout
    private lateinit var matchDateValueTextView: TextView
    private lateinit var team1EditText: EditText
    private lateinit var team2EditText: EditText
    private lateinit var groundNameEditText: EditText
    private lateinit var groundLocationEditText: EditText
    private lateinit var groundFeesEditText: EditText
    private lateinit var oversEditText: EditText
    private lateinit var ballProvidedRadioGroup: RadioGroup
    private lateinit var ballDetailsLayout: LinearLayout
    private lateinit var noOfBallsEditText: EditText
    private lateinit var ballNameSpinner: Spinner
    private lateinit var saveMatchButton: MaterialButton

    private var selectedMatchDateUtcMillis: Long? = null
    private var currentUserEmail: String = ""
    private var currentUserProfile: UserProfile? = null
    private var allUserProfiles: List<UserProfile> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_profile)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.team_profile_screen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        userProfileRepository = UserProfileRepository(application)
        currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        
        upcomingMatchFormCard = findViewById(R.id.upcomingMatchFormCard)
        upcomingMatchHeader = findViewById(R.id.upcomingMatchHeader)
        upcomingMatchContent = findViewById(R.id.upcomingMatchContent)
        upcomingMatchChevron = findViewById(R.id.upcomingMatchChevron)

        matchDetailsHeader = findViewById(R.id.matchDetailsHeader)
        matchDetailsContent = findViewById(R.id.matchDetailsContent)
        matchDetailsChevron = findViewById(R.id.matchDetailsChevron)

        playerAvailabilityHeader = findViewById(R.id.playerAvailabilityHeader)
        playerAvailabilityContent = findViewById(R.id.playerAvailabilityContent)
        playerAvailabilityChevron = findViewById(R.id.playerAvailabilityChevron)
        availabilityRecyclerView = findViewById(R.id.availabilityRecyclerView)
        emptyAvailabilityTextView = findViewById(R.id.emptyAvailabilityTextView)
        availabilityFilterGroup = findViewById(R.id.availabilityFilterGroup)

        teamMembersHeader = findViewById(R.id.teamMembersHeader)
        teamMembersContent = findViewById(R.id.teamMembersContent)
        teamMembersChevron = findViewById(R.id.teamMembersChevron)

        teamMembersRecyclerView = findViewById(R.id.teamMembersRecyclerView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)

        matchDateContainer = findViewById(R.id.matchDateContainer)
        matchDateValueTextView = findViewById(R.id.matchDateValueTextView)
        team1EditText = findViewById(R.id.team1EditText)
        team2EditText = findViewById(R.id.team2EditText)
        groundNameEditText = findViewById(R.id.groundNameEditText)
        groundLocationEditText = findViewById(R.id.groundLocationEditText)
        groundFeesEditText = findViewById(R.id.groundFeesEditText)
        oversEditText = findViewById(R.id.oversEditText)
        ballProvidedRadioGroup = findViewById(R.id.ballProvidedRadioGroup)
        ballDetailsLayout = findViewById(R.id.ballDetailsLayout)
        noOfBallsEditText = findViewById(R.id.noOfBallsEditText)
        ballNameSpinner = findViewById(R.id.ballNameSpinner)
        saveMatchButton = findViewById(R.id.saveMatchButton)

        // Setup ball name spinner
        val ballNames = listOf("SF Yorker", "SF True Test")
        val ballAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ballNames)
        ballAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ballNameSpinner.adapter = ballAdapter

        ballProvidedRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            ballDetailsLayout.visibility = if (checkedId == R.id.ballProvidedYes) View.VISIBLE else View.GONE
        }

        // Collapsed by default
        setExpanded(upcomingMatchContent, upcomingMatchChevron, expanded = false)
        setExpanded(matchDetailsContent, matchDetailsChevron, expanded = false)
        setExpanded(playerAvailabilityContent, playerAvailabilityChevron, expanded = false)
        setExpanded(teamMembersContent, teamMembersChevron, expanded = false)

        upcomingMatchHeader.setOnClickListener {
            val expand = upcomingMatchContent.visibility != View.VISIBLE
            setExpanded(upcomingMatchContent, upcomingMatchChevron, expand)
        }

        matchDetailsHeader.setOnClickListener {
            val expand = matchDetailsContent.visibility != View.VISIBLE
            setExpanded(matchDetailsContent, matchDetailsChevron, expand)
        }

        playerAvailabilityHeader.setOnClickListener {
            val expand = playerAvailabilityContent.visibility != View.VISIBLE
            setExpanded(playerAvailabilityContent, playerAvailabilityChevron, expand)
            if (expand) loadPlayerAvailability()
        }

        teamMembersHeader.setOnClickListener {
            val expand = teamMembersContent.visibility != View.VISIBLE
            setExpanded(teamMembersContent, teamMembersChevron, expand)
        }

        availabilityFilterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                loadPlayerAvailability(checkedId)
            }
        }

        setupUpcomingMatchForm()
        
        teamMembersRecyclerView.layoutManager = LinearLayoutManager(this)
        availabilityRecyclerView.layoutManager = LinearLayoutManager(this)
        
        loadData()
        
        setupBottomNavigation()
    }

    private fun setExpanded(content: View, chevron: ImageView, expanded: Boolean) {
        content.visibility = if (expanded) View.VISIBLE else View.GONE
        chevron.animate().rotation(if (expanded) 180f else 0f).setDuration(150).start()
    }

    private fun setupUpcomingMatchForm() {
        // Load local data first for immediate display
        UpcomingMatchStore.load(this)?.let { match ->
            displayMatchDetails(match)
        }

        matchDateContainer.setOnClickListener {
            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build()

            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_match_date))
                .setCalendarConstraints(constraints)
                .build()

            picker.addOnPositiveButtonClickListener { utcMillis ->
                selectedMatchDateUtcMillis = utcMillis
                val formatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                matchDateValueTextView.text = formatter.format(Date(utcMillis))
            }

            picker.show(supportFragmentManager, "match_date_picker")
        }

        saveMatchButton.setOnClickListener {
            val date = selectedMatchDateUtcMillis
            if (date == null) {
                Toast.makeText(this, getString(R.string.select_match_date), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val team1 = team1EditText.text?.toString().orEmpty().trim()
            val team2 = team2EditText.text?.toString().orEmpty().trim()
            val groundName = groundNameEditText.text?.toString().orEmpty().trim()
            val groundLocation = groundLocationEditText.text?.toString().orEmpty().trim()
            val groundFees = groundFeesEditText.text?.toString()?.toDoubleOrNull() ?: 0.0
            val overs = oversEditText.text?.toString()?.toIntOrNull() ?: 20
            val ballProvided = ballProvidedRadioGroup.checkedRadioButtonId == R.id.ballProvidedYes
            val noOfBalls = if (ballProvided) noOfBallsEditText.text?.toString()?.toIntOrNull() ?: 0 else 0
            val ballName = if (ballProvided) ballNameSpinner.selectedItem?.toString() else null

            var hasError = false
            if (team1.isBlank()) {
                team1EditText.error = "Required"
                hasError = true
            }
            if (team2.isBlank()) {
                team2EditText.error = "Required"
                hasError = true
            }
            if (groundName.isBlank()) {
                groundNameEditText.error = "Required"
                hasError = true
            }
            if (groundLocation.isBlank()) {
                groundLocationEditText.error = "Required"
                hasError = true
            }
            if (hasError) return@setOnClickListener

            val match = UpcomingMatch(
                dateUtcMillis = date,
                team1 = team1,
                team2 = team2,
                groundName = groundName,
                groundLocation = groundLocation,
                groundFees = groundFees,
                overs = overs,
                ballProvided = ballProvided,
                noOfBalls = noOfBalls,
                ballName = ballName
            )

            // Save locally
            UpcomingMatchStore.save(this, match)

            // Save to Firestore
            lifecycleScope.launch {
                try {
                    firestoreRepository.uploadUpcomingMatch(match)
                    Toast.makeText(this@TeamProfileActivity, "Match details synced to cloud", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@TeamProfileActivity, "Saved locally. Sync failed.", Toast.LENGTH_SHORT).show()
                }
            }

            Toast.makeText(this, "Upcoming match saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayMatchDetails(match: UpcomingMatch) {
        val formatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        selectedMatchDateUtcMillis = match.dateUtcMillis
        matchDateValueTextView.text = formatter.format(Date(match.dateUtcMillis))
        team1EditText.setText(match.team1)
        team2EditText.setText(match.team2)
        groundNameEditText.setText(match.groundName)
        groundLocationEditText.setText(match.groundLocation)
        groundFeesEditText.setText(match.groundFees.toString())
        oversEditText.setText(match.overs.toString())
        if (match.ballProvided) {
            ballProvidedRadioGroup.check(R.id.ballProvidedYes)
            noOfBallsEditText.setText(match.noOfBalls.toString())
            val ballIndex = listOf("SF Yorker", "SF True Test").indexOf(match.ballName)
            if (ballIndex != -1) ballNameSpinner.setSelection(ballIndex)
        } else {
            ballProvidedRadioGroup.check(R.id.ballProvidedNo)
        }
    }
    
    private fun loadData() {
        lifecycleScope.launch {
            try {
                // 1. Fetch latest match details from Firestore
                val remoteMatch = try {
                    firestoreRepository.downloadUpcomingMatch()
                } catch (e: Exception) {
                    null
                }
                
                remoteMatch?.let {
                    UpcomingMatchStore.save(this@TeamProfileActivity, it)
                    displayMatchDetails(it)
                }

                // 2. Fetch latest profiles from Firestore
                val firebaseProfiles = try {
                    firestoreRepository.downloadAllUserProfiles()
                } catch (e: Exception) {
                    emptyList()
                }
                
                // 3. Update local DB with Firestore data to ensure roles are current
                firebaseProfiles.forEach { profile ->
                    userProfileRepository.insertOrUpdate(profile)
                }

                // 4. Fetch all profiles including any local-only ones
                val localProfiles = try {
                    userProfileRepository.getAllUserProfiles()
                } catch (e: Exception) {
                    emptyList()
                }
                
                allUserProfiles = (firebaseProfiles + localProfiles).distinctBy { it.email }

                // 5. Identify current user's profile and roles using the LATEST data
                currentUserProfile = allUserProfiles.find { it.email.equals(currentUserEmail, ignoreCase = true) }
                    ?: userProfileRepository.getUserProfileSync(currentUserEmail)
                
                val isAdmin = currentUserProfile?.isAdmin() == true
                val canManageMatches = currentUserProfile?.canManageMatches() == true
                
                upcomingMatchFormCard.visibility = View.VISIBLE
                
                // Match Details editing is for authorized users only
                matchDetailsHeader.visibility = if (canManageMatches) View.VISIBLE else View.GONE
                matchDetailsContent.visibility = View.GONE 

                teamMemberAdapter = TeamMemberAdapter(isAdmin) { profile ->
                    val intent = Intent(this@TeamProfileActivity, com.magnum.cricketclub.ui.me.MeActivity::class.java)
                    intent.putExtra("edit_user_email", profile.email)
                    startActivity(intent)
                }
                teamMembersRecyclerView.adapter = teamMemberAdapter
                
                if (allUserProfiles.isEmpty()) {
                    emptyStateTextView.visibility = View.VISIBLE
                    teamMembersRecyclerView.visibility = View.GONE
                } else {
                    emptyStateTextView.visibility = View.GONE
                    teamMembersRecyclerView.visibility = View.VISIBLE
                    teamMemberAdapter.submitList(allUserProfiles)
                }

                // Initialize PlayerAvailabilityAdapter
                val namesMap = allUserProfiles.associate { it.email to (it.name ?: it.email) }
                playerAvailabilityAdapter = PlayerAvailabilityAdapter(namesMap)
                availabilityRecyclerView.adapter = playerAvailabilityAdapter
                
                // Load availability if the main section is expanded
                if (upcomingMatchContent.visibility == View.VISIBLE && playerAvailabilityContent.visibility == View.VISIBLE) {
                    loadPlayerAvailability()
                }

            } catch (e: Exception) {
                emptyStateTextView.visibility = View.VISIBLE
                teamMembersRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun loadPlayerAvailability(checkedId: Int = availabilityFilterGroup.checkedButtonId) {
        val matchDate = selectedMatchDateUtcMillis ?: return
        
        val availabilityFilter: Boolean? = when (checkedId) {
            R.id.filterAvailable -> true
            R.id.filterNotAvailable -> false
            else -> null
        }

        lifecycleScope.launch {
            try {
                val availabilityList = firestoreRepository.downloadAllMatchAvailabilities(matchDate, availabilityFilter)
                
                if (availabilityList.isEmpty()) {
                    emptyAvailabilityTextView.text = when (checkedId) {
                        R.id.filterAvailable -> "No players available"
                        R.id.filterNotAvailable -> "No players unavailable"
                        else -> "No availability updates yet"
                    }
                    emptyAvailabilityTextView.visibility = View.VISIBLE
                    availabilityRecyclerView.visibility = View.GONE
                } else {
                    emptyAvailabilityTextView.visibility = View.GONE
                    availabilityRecyclerView.visibility = View.VISIBLE
                    playerAvailabilityAdapter.submitList(availabilityList)
                }
            } catch (e: Exception) {
                emptyAvailabilityTextView.visibility = View.VISIBLE
                availabilityRecyclerView.visibility = View.GONE
                Toast.makeText(this@TeamProfileActivity, "Failed to load availability", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
