package com.magnum.cricketclub.ui.teamprofile

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.*
import com.magnum.cricketclub.data.remote.FirestoreMatchAvailability
import com.magnum.cricketclub.data.remote.FirestoreRepository
import com.magnum.cricketclub.data.sync.SyncService
import com.magnum.cricketclub.ui.BaseActivity
import com.magnum.cricketclub.utils.UpcomingMatchStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

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
    private lateinit var btnExportAvailability: MaterialButton

    private lateinit var teamMembersHeader: LinearLayout
    private lateinit var teamMembersContent: LinearLayout
    private lateinit var teamMembersChevron: ImageView

    private lateinit var teamMembersRecyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var teamMemberAdapter: TeamMemberAdapter
    private lateinit var userProfileRepository: UserProfileRepository
    private val firestoreRepository = FirestoreRepository()
    private lateinit var syncService: SyncService
    private val configRepository by lazy { AppConfigRepository(AppDatabase.getDatabase(application).appConfigDao()) }

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

    // App Users section
    private lateinit var appUsersCard: View
    private lateinit var appUsersHeader: LinearLayout
    private lateinit var appUsersContent: LinearLayout
    private lateinit var appUsersChevron: ImageView
    private lateinit var btnAddUser: MaterialButton
    private lateinit var appUsersRecyclerView: RecyclerView
    private lateinit var appUsersAdapter: TeamMemberAdapter

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
        syncService = SyncService(applicationContext)
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
        btnExportAvailability = findViewById(R.id.btnExportAvailability)

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

        // App Users section binding
        appUsersCard = findViewById(R.id.appUsersCard)
        appUsersHeader = findViewById(R.id.appUsersHeader)
        appUsersContent = findViewById(R.id.appUsersContent)
        appUsersChevron = findViewById(R.id.appUsersChevron)
        btnAddUser = findViewById(R.id.btnAddUser)
        appUsersRecyclerView = findViewById(R.id.appUsersRecyclerView)

        // Setup ball name spinner
        val ballNames = listOf("SF Yorker", "SF True Test")
        val ballAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ballNames)
        ballAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ballNameSpinner.adapter = ballAdapter

        ballProvidedRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            ballDetailsLayout.visibility = if (checkedId == R.id.ballProvidedYes) View.VISIBLE else View.GONE
        }

        // Collapsed by default (except Match Details per request)
        setExpanded(upcomingMatchContent, upcomingMatchChevron, expanded = true)
        setExpanded(matchDetailsContent, matchDetailsChevron, expanded = true)
        setExpanded(playerAvailabilityContent, playerAvailabilityChevron, expanded = false)
        setExpanded(teamMembersContent, teamMembersChevron, expanded = false)
        setExpanded(appUsersContent, appUsersChevron, expanded = false)

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

        appUsersHeader.setOnClickListener {
            val expand = appUsersContent.visibility != View.VISIBLE
            setExpanded(appUsersContent, appUsersChevron, expand)
        }

        availabilityFilterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                loadPlayerAvailability(checkedId)
            }
        }

        btnExportAvailability.setOnClickListener {
            exportAvailabilityReport()
        }

        setupUpcomingMatchForm()
        setupAppUsersSection()
        
        teamMembersRecyclerView.layoutManager = LinearLayoutManager(this)
        appUsersRecyclerView.layoutManager = LinearLayoutManager(this)
        availabilityRecyclerView.layoutManager = LinearLayoutManager(this)
        
        loadData()
        
        setupBottomNavigation()
    }

    private fun setExpanded(content: View, chevron: ImageView, expanded: Boolean) {
        content.visibility = if (expanded) View.VISIBLE else View.GONE
        chevron.animate().rotation(if (expanded) 180f else 0f).setDuration(150).start()
    }

    private fun setupAppUsersSection() {
        btnAddUser.setOnClickListener {
            showEditUserDialog(null) // Show empty dialog for new user
        }
    }

    private fun showEditUserDialog(profile: UserProfile?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_user, null)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.nameEditText)
        val emailEditText = dialogView.findViewById<TextInputEditText>(R.id.emailEditText)
        val mobileEditText = dialogView.findViewById<TextInputEditText>(R.id.mobileEditText)
        val alternateMobileEditText = dialogView.findViewById<TextInputEditText>(R.id.alternateMobileEditText)
        val preferenceSpinner = dialogView.findViewById<Spinner>(R.id.preferenceSpinner)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)

        // Responsibilities Checkboxes
        val cbAppOwner = dialogView.findViewById<CheckBox>(R.id.cbAppOwner)
        val cbAppDeveloper = dialogView.findViewById<CheckBox>(R.id.cbAppDeveloper)
        val cbManager = dialogView.findViewById<CheckBox>(R.id.cbManager)
        val cbSecretary = dialogView.findViewById<CheckBox>(R.id.cbSecretary)
        val cbCaptain = dialogView.findViewById<CheckBox>(R.id.cbCaptain)
        val cbViceCaptain = dialogView.findViewById<CheckBox>(R.id.cbViceCaptain)
        val cbPlayer = dialogView.findViewById<CheckBox>(R.id.cbPlayer)
        val cbFinanceMaintenance = dialogView.findViewById<CheckBox>(R.id.cbFinanceMaintenance)
        val cbFinanceContributor = dialogView.findViewById<CheckBox>(R.id.cbFinanceContributor)

        val checkboxMap = mapOf(
            "App Owner" to cbAppOwner,
            "App Developer" to cbAppDeveloper,
            "Manager" to cbManager,
            "Secretary" to cbSecretary,
            "Captain" to cbCaptain,
            "Vice Captain" to cbViceCaptain,
            "Player" to cbPlayer,
            "Finance Maintenance" to cbFinanceMaintenance,
            "Finance Contributor" to cbFinanceContributor
        )

        // Setup preference spinner
        val preferences = listOf("Batsman", "Bowler", "All Rounder", "Wicket Keeper")
        val prefAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, preferences)
        prefAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        preferenceSpinner.adapter = prefAdapter

        if (profile != null) {
            dialogTitle.text = "Edit User"
            nameEditText.setText(profile.name)
            emailEditText.setText(profile.email)
            emailEditText.isEnabled = false // Email is primary key
            mobileEditText.setText(profile.mobileNumber)
            alternateMobileEditText.setText(profile.alternateMobileNumber)
            
            val prefIndex = preferences.indexOf(profile.playerPreference)
            if (prefIndex >= 0) preferenceSpinner.setSelection(prefIndex)

            // Set Checkboxes
            val currentResponsibilities = profile.additionalResponsibility?.split(",")?.map { it.trim() } ?: emptyList()
            checkboxMap.forEach { (name, cb) ->
                cb.isChecked = currentResponsibilities.contains(name)
            }
        } else {
            dialogTitle.text = "Add New User"
            cbPlayer.isChecked = true // Default for new user
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val email = emailEditText.text.toString().trim()
                val mobile = mobileEditText.text.toString().trim()
                val alternateMobile = alternateMobileEditText.text.toString().trim()
                val preference = preferenceSpinner.selectedItem.toString()

                // Collect Responsibilities
                val selectedResponsibilities = checkboxMap.filter { it.value.isChecked }.keys.joinToString(", ")

                if (email.isEmpty()) {
                    Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newProfile = UserProfile(
                    email = email,
                    name = name,
                    playerPreference = preference,
                    mobileNumber = mobile,
                    alternateMobileNumber = alternateMobile,
                    additionalResponsibility = selectedResponsibilities
                )

                lifecycleScope.launch {
                    try {
                        // Save to local DB
                        userProfileRepository.insertOrUpdate(newProfile)
                        // Sync to Firestore
                        syncService.syncUserProfile(newProfile)
                        
                        loadData() // Refresh lists
                        Toast.makeText(this@TeamProfileActivity, "User saved successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@TeamProfileActivity, "Error saving user: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
                // 1. Fetch latest from Firestore first using SyncService
                syncService.syncFromFirestore()

                // 2. Fetch latest match details from Firestore
                val remoteMatch = try {
                    firestoreRepository.downloadUpcomingMatch()
                } catch (e: Exception) {
                    null
                }
                
                remoteMatch?.let {
                    UpcomingMatchStore.save(this@TeamProfileActivity, it)
                    displayMatchDetails(it)
                }

                // 3. Fetch all profiles from local DB (already updated by syncFromFirestore)
                allUserProfiles = try {
                    userProfileRepository.getAllUserProfiles()
                } catch (e: Exception) {
                    emptyList()
                }

                // 4. Identify current user's profile and roles using the LATEST data
                currentUserProfile = allUserProfiles.find { it.email.equals(currentUserEmail, ignoreCase = true) }
                    ?: userProfileRepository.getUserProfile(currentUserEmail).firstOrNull()
                
                val isAdmin = currentUserProfile?.isAdmin() == true
                val canManageMatches = currentUserProfile?.canManageMatches() == true
                
                upcomingMatchFormCard.visibility = View.VISIBLE
                
                // Match Details must be visible to all users
                matchDetailsHeader.visibility = View.VISIBLE
                
                // Only managers can edit
                updateMatchDetailsEditability(canManageMatches)

                // App Users section is for App Owner/Developer ONLY
                appUsersCard.visibility = if (isAdmin) View.VISIBLE else View.GONE

                // Setup Adapters
                teamMemberAdapter = TeamMemberAdapter(isAdmin, 
                    onEditClick = { profile ->
                        showEditUserDialog(profile)
                    },
                    onDeleteClick = { profile ->
                        showDeleteUserDialog(profile)
                    }
                )
                teamMembersRecyclerView.adapter = teamMemberAdapter

                appUsersAdapter = TeamMemberAdapter(isAdmin,
                    onEditClick = { profile ->
                        showEditUserDialog(profile)
                    },
                    onDeleteClick = { profile ->
                        showDeleteUserDialog(profile)
                    }
                )
                appUsersRecyclerView.adapter = appUsersAdapter
                
                if (allUserProfiles.isEmpty()) {
                    emptyStateTextView.visibility = View.VISIBLE
                    teamMembersRecyclerView.visibility = View.GONE
                } else {
                    emptyStateTextView.visibility = View.GONE
                    teamMembersRecyclerView.visibility = View.VISIBLE
                    teamMemberAdapter.submitList(allUserProfiles)
                    appUsersAdapter.submitList(allUserProfiles)
                }

                // Initialize PlayerAvailabilityAdapter
                val namesMap = allUserProfiles.associate { it.email to (it.name ?: it.email) }
                playerAvailabilityAdapter = PlayerAvailabilityAdapter(namesMap) { availability ->
                    if (canManageMatches) {
                        showUpdateAvailabilityDialog(availability)
                    }
                }
                availabilityRecyclerView.adapter = playerAvailabilityAdapter
                
                // Load availability if the main section is expanded
                if (playerAvailabilityContent.visibility == View.VISIBLE) {
                    loadPlayerAvailability()
                }

            } catch (e: Exception) {
                emptyStateTextView.visibility = View.VISIBLE
                teamMembersRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun showUpdateAvailabilityDialog(availability: FirestoreMatchAvailability) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_availability, null)
        val nameTextView = dialogView.findViewById<TextView>(R.id.playerNameTextView)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.availabilityRadioGroup)
        val rbAvailable = dialogView.findViewById<RadioButton>(R.id.rbAvailable)
        val rbNotAvailable = dialogView.findViewById<RadioButton>(R.id.rbNotAvailable)
        val reasonInputLayout = dialogView.findViewById<View>(R.id.reasonInputLayout)
        val reasonEditText = dialogView.findViewById<EditText>(R.id.reasonEditText)

        nameTextView.text = "Player: ${allUserProfiles.find { it.email == availability.userEmail }?.name ?: availability.userEmail}"
        
        if (availability.lastModified != 0L) {
            if (availability.available) rbAvailable.isChecked = true
            else rbNotAvailable.isChecked = true
            reasonEditText.setText(availability.reason)
            reasonInputLayout.visibility = if (availability.available) View.GONE else View.VISIBLE
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            reasonInputLayout.visibility = if (checkedId == R.id.rbNotAvailable) View.VISIBLE else View.GONE
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val isAvailable = rbAvailable.isChecked
                val reason = if (!isAvailable) reasonEditText.text.toString().trim() else null
                
                if (!isAvailable && reason.isNullOrBlank()) {
                    Toast.makeText(this, "Please provide a reason", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        firestoreRepository.uploadMatchAvailability(
                            isAvailable, 
                            availability.matchDate, 
                            reason, 
                            availability.userEmail
                        )
                        loadPlayerAvailability()
                        Toast.makeText(this@TeamProfileActivity, "Availability updated", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@TeamProfileActivity, "Failed to update", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateMatchDetailsEditability(canEdit: Boolean) {
        matchDateContainer.isEnabled = canEdit
        team1EditText.isEnabled = canEdit
        team2EditText.isEnabled = canEdit
        groundNameEditText.isEnabled = canEdit
        groundLocationEditText.isEnabled = canEdit
        groundFeesEditText.isEnabled = canEdit
        oversEditText.isEnabled = canEdit
        
        for (i in 0 until ballProvidedRadioGroup.childCount) {
            ballProvidedRadioGroup.getChildAt(i).isEnabled = canEdit
        }
        
        noOfBallsEditText.isEnabled = canEdit
        ballNameSpinner.isEnabled = canEdit
        
        saveMatchButton.visibility = if (canEdit) View.VISIBLE else View.GONE
    }

    private fun showDeleteUserDialog(profile: UserProfile) {
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete user ${profile.name ?: profile.email}? This will remove them from the club roster.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        // Delete from Firestore
                        firestoreRepository.deleteUserProfile(profile.email)
                        // Delete from local DB
                        userProfileRepository.delete(profile.email)
                        
                        loadData()
                        Toast.makeText(this@TeamProfileActivity, "User deleted", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@TeamProfileActivity, "Error deleting user: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadPlayerAvailability(checkedId: Int = availabilityFilterGroup.checkedButtonId) {
        val matchDate = selectedMatchDateUtcMillis ?: return
        
        lifecycleScope.launch {
            try {
                val confirmedAvailabilities = firestoreRepository.downloadAllMatchAvailabilities(matchDate, null)
                val confirmedMap = confirmedAvailabilities.associateBy { it.userEmail }
                
                // Build a list of ALL players with their confirmed status or "Pending"
                val allPlayersAvailability = allUserProfiles.map { profile ->
                    confirmedMap[profile.email] ?: FirestoreMatchAvailability(
                        userEmail = profile.email,
                        matchDate = matchDate,
                        lastModified = 0L // Marker for "Not Confirmed"
                    )
                }

                val filteredList = when (checkedId) {
                    R.id.filterAvailable -> allPlayersAvailability.filter { it.lastModified != 0L && it.available }
                    R.id.filterNotAvailable -> allPlayersAvailability.filter { it.lastModified != 0L && !it.available }
                    else -> allPlayersAvailability // All includes Pending
                }
                
                if (filteredList.isEmpty()) {
                    emptyAvailabilityTextView.text = when (checkedId) {
                        R.id.filterAvailable -> "No players available"
                        R.id.filterNotAvailable -> "No players unavailable"
                        else -> "No players in team"
                    }
                    emptyAvailabilityTextView.visibility = View.VISIBLE
                    availabilityRecyclerView.visibility = View.GONE
                } else {
                    emptyAvailabilityTextView.visibility = View.GONE
                    availabilityRecyclerView.visibility = View.VISIBLE
                    playerAvailabilityAdapter.submitList(filteredList)
                }
            } catch (e: Exception) {
                emptyAvailabilityTextView.visibility = View.VISIBLE
                availabilityRecyclerView.visibility = View.GONE
                Toast.makeText(this@TeamProfileActivity, "Failed to load availability", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportAvailabilityReport() {
        val matchDate = selectedMatchDateUtcMillis ?: return
        lifecycleScope.launch {
            try {
                val confirmedAvailabilities = firestoreRepository.downloadAllMatchAvailabilities(matchDate, null)
                val confirmedMap = confirmedAvailabilities.associateBy { it.userEmail }
                
                val allPlayersAvailability = allUserProfiles.map { profile ->
                    confirmedMap[profile.email] ?: FirestoreMatchAvailability(
                        userEmail = profile.email,
                        matchDate = matchDate,
                        lastModified = 0L
                    )
                }

                // Sorted with status: Available, Not available, then Pending
                val sortedList = allPlayersAvailability.sortedWith(compareBy<FirestoreMatchAvailability> { 
                    when {
                        it.lastModified == 0L -> 2 // Pending
                        it.available -> 0 // Available
                        else -> 1 // Not Available
                    }
                }.thenBy { allUserProfiles.find { p -> p.email == it.userEmail }?.name ?: it.userEmail })

                val match = UpcomingMatchStore.load(this@TeamProfileActivity)
                generateAvailabilityPdf(sortedList, match)
            } catch (e: Exception) {
                Toast.makeText(this@TeamProfileActivity, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateAvailabilityPdf(availabilities: List<FirestoreMatchAvailability>, match: UpcomingMatch?) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
        }
        val statsPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
        }
        val textPaint = Paint().apply {
            textSize = 10f
        }

        var yPos = 40f
        canvas.drawText("Magnum Cricket Club - Player Availability", 40f, yPos, titlePaint)
        yPos += 30f

        if (match != null) {
            val formatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            canvas.drawText("Match: ${match.team1} vs ${match.team2}", 40f, yPos, headerPaint)
            yPos += 20f
            canvas.drawText("Date: ${formatter.format(Date(match.dateUtcMillis))}", 40f, yPos, headerPaint)
            yPos += 20f
            canvas.drawText("Ground: ${match.groundName}", 40f, yPos, headerPaint)
            yPos += 30f
        }

        // Add Stats Summary
        val availableCount = availabilities.count { it.lastModified != 0L && it.available }
        val notAvailableCount = availabilities.count { it.lastModified != 0L && !it.available }
        
        statsPaint.color = Color.parseColor("#4CAF50") // Green
        canvas.drawText("Available Players: $availableCount", 40f, yPos, statsPaint)
        
        statsPaint.color = Color.parseColor("#F44336") // Red
        val notAvailableWidth = statsPaint.measureText("Available Players: $availableCount") + 40
        canvas.drawText("Not Available Players: $notAvailableCount", 40f + notAvailableWidth, yPos, statsPaint)
        
        yPos += 40f
        paint.color = Color.BLACK

        // Table headers
        canvas.drawText("Player Name", 40f, yPos, headerPaint)
        canvas.drawText("Status", 250f, yPos, headerPaint)
        canvas.drawText("Reason", 380f, yPos, headerPaint)
        yPos += 10f
        canvas.drawLine(40f, yPos, 550f, yPos, paint)
        yPos += 20f

        for (item in availabilities) {
            if (yPos > 800) break
            
            val name = allUserProfiles.find { it.email == item.userEmail }?.name ?: item.userEmail
            canvas.drawText(name, 40f, yPos, textPaint)
            
            val status = when {
                item.lastModified == 0L -> "Not Confirmed"
                item.available -> "Available"
                else -> "Not Available"
            }
            canvas.drawText(status, 250f, yPos, textPaint)
            
            val reason = item.reason ?: ""
            val reasonDisplay = if (reason.length > 30) reason.substring(0, 27) + "..." else reason
            canvas.drawText(reasonDisplay, 380f, yPos, textPaint)
            
            yPos += 20f
        }

        pdfDocument.finishPage(page)

        val fileName = "Availability_Report_${System.currentTimeMillis()}.pdf"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    Toast.makeText(this, "Report saved to Downloads", Toast.LENGTH_LONG).show()
                    openPdf(uri)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                Toast.makeText(this, "Report saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                openPdf(Uri.fromFile(file))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun openPdf(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(intent, "Open Report"))
        } catch (e: Exception) {
            Toast.makeText(this, "No app to open PDF", Toast.LENGTH_SHORT).show()
        }
    }
}
