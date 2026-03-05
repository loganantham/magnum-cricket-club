package com.magnum.cricketclub.ui.teamprofile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.UpcomingMatch
import com.magnum.cricketclub.data.UserProfile
import com.magnum.cricketclub.data.UserProfileRepository
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
        setExpanded(teamMembersContent, teamMembersChevron, expanded = false)

        upcomingMatchHeader.setOnClickListener {
            val expand = upcomingMatchContent.visibility != View.VISIBLE
            setExpanded(upcomingMatchContent, upcomingMatchChevron, expand)
        }

        teamMembersHeader.setOnClickListener {
            val expand = teamMembersContent.visibility != View.VISIBLE
            setExpanded(teamMembersContent, teamMembersChevron, expand)
        }

        setupUpcomingMatchForm()
        
        teamMembersRecyclerView.layoutManager = LinearLayoutManager(this)
        
        loadTeamMembers()
        
        setupBottomNavigation()
    }

    private fun setExpanded(content: View, chevron: ImageView, expanded: Boolean) {
        content.visibility = if (expanded) View.VISIBLE else View.GONE
        chevron.animate().rotation(if (expanded) 180f else 0f).setDuration(150).start()
    }

    private fun setupUpcomingMatchForm() {
        val formatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        UpcomingMatchStore.load(this)?.let { match ->
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

            UpcomingMatchStore.save(
                this,
                UpcomingMatch(
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
            )

            Toast.makeText(this, "Upcoming match saved", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadTeamMembers() {
        lifecycleScope.launch {
            try {
                // 1. Fetch latest profiles from Firestore first
                val firebaseProfiles = try {
                    firestoreRepository.downloadAllUserProfiles()
                } catch (e: Exception) {
                    emptyList()
                }
                
                // 2. Update local DB with Firestore data to ensure roles are current
                firebaseProfiles.forEach { profile ->
                    userProfileRepository.insertOrUpdate(profile)
                }

                // 3. Fetch all profiles including any local-only ones
                val localProfiles = try {
                    userProfileRepository.getAllUserProfiles()
                } catch (e: Exception) {
                    emptyList()
                }
                
                val allProfiles = (firebaseProfiles + localProfiles).distinctBy { it.email }

                // 4. Identify current user's profile and roles using the LATEST data
                currentUserProfile = allProfiles.find { it.email.equals(currentUserEmail, ignoreCase = true) }
                    ?: userProfileRepository.getUserProfileSync(currentUserEmail)
                
                val canManageMatches = currentUserProfile?.canManageMatches() == true
                val isAdmin = currentUserProfile?.isAdmin() == true
                
                // 5. Update UI based on roles
                upcomingMatchFormCard.visibility = if (canManageMatches) View.VISIBLE else View.GONE

                teamMemberAdapter = TeamMemberAdapter(isAdmin) { profile ->
                    val intent = Intent(this@TeamProfileActivity, com.magnum.cricketclub.ui.me.MeActivity::class.java)
                    intent.putExtra("edit_user_email", profile.email)
                    startActivity(intent)
                }
                teamMembersRecyclerView.adapter = teamMemberAdapter
                
                if (allProfiles.isEmpty()) {
                    emptyStateTextView.visibility = View.VISIBLE
                    teamMembersRecyclerView.visibility = View.GONE
                } else {
                    emptyStateTextView.visibility = View.GONE
                    teamMembersRecyclerView.visibility = View.VISIBLE
                    teamMemberAdapter.submitList(allProfiles)
                }
            } catch (e: Exception) {
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
