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
import android.text.Editable
import android.text.TextWatcher
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
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.*
import com.magnum.cricketclub.data.remote.FirestoreMatchAvailability
import com.magnum.cricketclub.data.remote.FirestoreRepository
import com.magnum.cricketclub.data.sync.SyncService
import com.magnum.cricketclub.ui.BaseActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class TeamProfileActivity : BaseActivity() {
    private lateinit var upcomingMatchHeader: LinearLayout
    private lateinit var upcomingMatchContent: View
    private lateinit var upcomingMatchChevron: ImageView
    private lateinit var upcomingMatchFormCard: View
    private lateinit var btnAddMatch: MaterialButton
    private lateinit var rvScheduledMatches: RecyclerView
    private lateinit var noMatchesText: TextView
    private lateinit var scheduledMatchAdapter: ScheduledMatchAdapter
    private lateinit var syncProgressBar: ProgressBar

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

    private lateinit var appUsersCard: View
    private lateinit var appUsersHeader: LinearLayout
    private lateinit var appUsersContent: LinearLayout
    private lateinit var appUsersChevron: ImageView
    private lateinit var btnAddUser: MaterialButton
    private lateinit var appUsersRecyclerView: RecyclerView
    private lateinit var appUsersAdapter: TeamMemberAdapter

    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var upcomingMatchDao: UpcomingMatchDao
    private val firestoreRepository = FirestoreRepository()
    private lateinit var syncService: SyncService
    
    private var currentUserEmail: String = ""
    private var currentUserProfile: UserProfile? = null
    private var allUserProfiles: List<UserProfile> = emptyList()
    private var allMatches: List<UpcomingMatch> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_profile)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.team_profile_screen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        val database = AppDatabase.getDatabase(application)
        userProfileRepository = UserProfileRepository(application)
        upcomingMatchDao = database.upcomingMatchDao()
        syncService = SyncService(applicationContext)
        currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        
        // Schedule Matches Section
        upcomingMatchFormCard = findViewById(R.id.upcomingMatchFormCard)
        upcomingMatchHeader = findViewById(R.id.upcomingMatchHeader)
        upcomingMatchContent = findViewById(R.id.upcomingMatchContent)
        upcomingMatchChevron = findViewById(R.id.upcomingMatchChevron)
        btnAddMatch = findViewById(R.id.btnAddMatch)
        rvScheduledMatches = findViewById(R.id.rvScheduledMatches)
        noMatchesText = findViewById(R.id.noMatchesText)
        syncProgressBar = findViewById(R.id.syncProgressBar)

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

        appUsersCard = findViewById(R.id.appUsersCard)
        appUsersHeader = findViewById(R.id.appUsersHeader)
        appUsersContent = findViewById(R.id.appUsersContent)
        appUsersChevron = findViewById(R.id.appUsersChevron)
        btnAddUser = findViewById(R.id.btnAddUser)
        appUsersRecyclerView = findViewById(R.id.appUsersRecyclerView)

        // Setup Click Listeners
        upcomingMatchHeader.setOnClickListener {
            val expand = upcomingMatchContent.visibility != View.VISIBLE
            setExpanded(upcomingMatchContent, upcomingMatchChevron, expand)
        }

        playerAvailabilityHeader.setOnClickListener {
            val expand = playerAvailabilityContent.visibility != View.VISIBLE
            setExpanded(playerAvailabilityContent, playerAvailabilityChevron, expand)
        }

        teamMembersHeader.setOnClickListener {
            val expand = teamMembersContent.visibility != View.VISIBLE
            setExpanded(teamMembersContent, teamMembersChevron, expand)
        }

        appUsersHeader.setOnClickListener {
            val expand = appUsersContent.visibility != View.VISIBLE
            setExpanded(appUsersContent, appUsersChevron, expand)
        }

        btnAddMatch.setOnClickListener { showAddMatchDialog(null) }
        
        btnAddUser.setOnClickListener { showEditUserDialog(null) }

        availabilityFilterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) loadPlayerAvailability(checkedId)
        }

        btnExportAvailability.setOnClickListener { exportAvailabilityReport() }

        rvScheduledMatches.layoutManager = LinearLayoutManager(this)
        teamMembersRecyclerView.layoutManager = LinearLayoutManager(this)
        appUsersRecyclerView.layoutManager = LinearLayoutManager(this)
        availabilityRecyclerView.layoutManager = LinearLayoutManager(this)

        setExpanded(upcomingMatchContent, upcomingMatchChevron, true)
        
        loadData()
        observeSyncStatus()
        setupBottomNavigation()
    }

    private fun setExpanded(content: View, chevron: ImageView, expanded: Boolean) {
        content.visibility = if (expanded) View.VISIBLE else View.GONE
        chevron.animate().rotation(if (expanded) 180f else 0f).setDuration(150).start()
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                syncService.syncFromFirestore()
                
                allUserProfiles = userProfileRepository.getAllUserProfiles()
                currentUserProfile = allUserProfiles.find { it.email.equals(currentUserEmail, ignoreCase = true) }
                
                val isAdmin = currentUserProfile?.isAdmin() == true
                val canManageMatches = currentUserProfile?.canManageMatches() == true

                updateAdminUI(isAdmin)

                upcomingMatchDao.getAllMatches().collect { matches ->
                    allMatches = matches
                    updateMatchesUI(canManageMatches)
                }

            } catch (e: Exception) {
                Toast.makeText(this@TeamProfileActivity, "Error loading data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeSyncStatus() {
        lifecycleScope.launch {
            syncService.syncStatus.collectLatest { status ->
                when (status) {
                    SyncService.SyncStatus.SYNCING -> {
                        syncProgressBar.visibility = View.VISIBLE
                        rvScheduledMatches.alpha = 0.5f
                    }
                    else -> {
                        syncProgressBar.visibility = View.GONE
                        rvScheduledMatches.alpha = 1.0f
                    }
                }
            }
        }
    }

    private fun updateAdminUI(isAdmin: Boolean) {
        teamMemberAdapter = TeamMemberAdapter(isAdmin, { showEditUserDialog(it) }, { showDeleteUserDialog(it) })
        teamMembersRecyclerView.adapter = teamMemberAdapter
        
        appUsersAdapter = TeamMemberAdapter(isAdmin, { showEditUserDialog(it) }, { showDeleteUserDialog(it) })
        appUsersRecyclerView.adapter = appUsersAdapter
        
        if (allUserProfiles.isNotEmpty()) {
            teamMemberAdapter.submitList(allUserProfiles)
            appUsersAdapter.submitList(allUserProfiles)
            emptyStateTextView.visibility = View.GONE
        }
        
        appUsersCard.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun updateMatchesUI(canManage: Boolean) {
        btnAddMatch.visibility = if (canManage) View.VISIBLE else View.GONE
        
        scheduledMatchAdapter = ScheduledMatchAdapter(canManage, 
            onEditClick = { showAddMatchDialog(it) },
            onDeleteClick = { showDeleteMatchDialog(it) }
        )
        rvScheduledMatches.adapter = scheduledMatchAdapter
        
        if (allMatches.isEmpty()) {
            noMatchesText.visibility = View.VISIBLE
            rvScheduledMatches.visibility = View.GONE
        } else {
            noMatchesText.visibility = View.GONE
            rvScheduledMatches.visibility = View.VISIBLE
            scheduledMatchAdapter.submitList(allMatches)
        }

        if (allMatches.isNotEmpty()) {
            loadPlayerAvailability()
        }
    }

    private fun showAddMatchDialog(existingMatch: UpcomingMatch?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_match, null)
        val title = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.matchDateEditText)
        val etTeam1 = dialogView.findViewById<TextInputEditText>(R.id.team1EditText)
        val etTeam2 = dialogView.findViewById<TextInputEditText>(R.id.team2EditText)
        val etGroundName = dialogView.findViewById<TextInputEditText>(R.id.groundNameEditText)
        val etGroundLoc = dialogView.findViewById<TextInputEditText>(R.id.groundLocationEditText)
        val etFees = dialogView.findViewById<TextInputEditText>(R.id.groundFeesEditText)
        val etOvers = dialogView.findViewById<TextInputEditText>(R.id.oversEditText)
        
        val rgMatchType = dialogView.findViewById<RadioGroup>(R.id.matchTypeRadioGroup)
        val layoutMagnumMatch = dialogView.findViewById<View>(R.id.magnumMatchOptions)
        val layoutMagnumGround = dialogView.findViewById<View>(R.id.magnumGroundMatchOptions)
        
        val rgShared = dialogView.findViewById<RadioGroup>(R.id.groundFeesSharedRadioGroup)
        val cbTeam1 = dialogView.findViewById<CheckBox>(R.id.cbTeam1Fees)
        val cbTeam2 = dialogView.findViewById<CheckBox>(R.id.cbTeam2Fees)

        var selectedDateMillis = existingMatch?.dateUtcMillis ?: 0L

        fun updateCheckboxLabels() {
            val t1 = etTeam1.text.toString().trim().ifEmpty { "Team 1" }
            val t2 = etTeam2.text.toString().trim().ifEmpty { "Team 2" }
            cbTeam1.text = getString(R.string.team1_fees_label, t1)
            cbTeam2.text = getString(R.string.team2_fees_label, t2)
        }

        val teamWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateCheckboxLabels() }
        }
        etTeam1.addTextChangedListener(teamWatcher)
        etTeam2.addTextChangedListener(teamWatcher)

        if (existingMatch != null) {
            title.text = getString(R.string.edit_upcoming_match)
            val fmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
            etDate.setText(fmt.format(Date(existingMatch.dateUtcMillis)))
            etTeam1.setText(existingMatch.team1)
            etTeam2.setText(existingMatch.team2)
            etGroundName.setText(existingMatch.groundName)
            etGroundLoc.setText(existingMatch.groundLocation)
            etFees.setText(existingMatch.groundFees.toString())
            etOvers.setText(existingMatch.overs.toString())
            
            if (existingMatch.matchType == "MAGNUM_GROUND_MATCH") {
                rgMatchType.check(R.id.rbMagnumGroundMatch)
                layoutMagnumMatch.visibility = View.GONE
                layoutMagnumGround.visibility = View.VISIBLE
                cbTeam1.isChecked = existingMatch.team1FeesCollected
                cbTeam2.isChecked = existingMatch.team2FeesCollected
            } else {
                rgMatchType.check(R.id.rbMagnumMatch)
                layoutMagnumMatch.visibility = View.VISIBLE
                layoutMagnumGround.visibility = View.GONE
                rgShared.check(if (existingMatch.groundFeesShared) R.id.rbFeesSharedYes else R.id.rbFeesSharedNo)
            }
        }
        updateCheckboxLabels()

        rgMatchType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbMagnumMatch) {
                layoutMagnumMatch.visibility = View.VISIBLE
                layoutMagnumGround.visibility = View.GONE
            } else {
                layoutMagnumMatch.visibility = View.GONE
                layoutMagnumGround.visibility = View.VISIBLE
            }
        }

        etDate.setOnClickListener {
            val constraints = CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build()
            val picker = MaterialDatePicker.Builder.datePicker().setTitleText(getString(R.string.select_match_date)).setCalendarConstraints(constraints).build()
            picker.addOnPositiveButtonClickListener { 
                selectedDateMillis = it
                val fmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
                etDate.setText(fmt.format(Date(it)))
            }
            picker.show(supportFragmentManager, "date_picker")
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val team1 = etTeam1.text.toString().trim()
                val team2 = etTeam2.text.toString().trim()
                if (team1.isEmpty() || team2.isEmpty() || selectedDateMillis == 0L) {
                    Toast.makeText(this, "Date and Teams are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val semanticId = UpcomingMatch.generateSemanticId(selectedDateMillis, team1, team2)
                val matchType = if (rgMatchType.checkedRadioButtonId == R.id.rbMagnumMatch) "MAGNUM_MATCH" else "MAGNUM_GROUND_MATCH"
                
                lifecycleScope.launch {
                    if (existingMatch != null && existingMatch.id != semanticId) {
                        upcomingMatchDao.deleteMatch(existingMatch)
                        syncService.deleteUpcomingMatchFromFirestore(existingMatch.id)
                    }

                    val match = (existingMatch ?: UpcomingMatch(id = semanticId, dateUtcMillis = 0, team1 = "", team2 = "", groundName = "", groundLocation = "")).copy(
                        id = semanticId,
                        dateUtcMillis = selectedDateMillis,
                        team1 = team1,
                        team2 = team2,
                        groundName = etGroundName.text.toString().trim(),
                        groundLocation = etGroundLoc.text.toString().trim(),
                        groundFees = etFees.text.toString().toDoubleOrNull() ?: 0.0,
                        overs = etOvers.text.toString().toIntOrNull() ?: 20,
                        matchType = matchType,
                        groundFeesShared = rgShared.checkedRadioButtonId == R.id.rbFeesSharedYes,
                        team1FeesCollected = cbTeam1.isChecked,
                        team2FeesCollected = cbTeam2.isChecked
                    )

                    upcomingMatchDao.insertMatch(match)
                    syncService.syncUpcomingMatch(match)
                    Toast.makeText(this@TeamProfileActivity, "Match scheduled", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteMatchDialog(match: UpcomingMatch) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete))
            .setMessage("Are you sure you want to delete this match? This will remove it from the schedule and maintenance list.")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lifecycleScope.launch {
                    upcomingMatchDao.deleteMatch(match)
                    syncService.deleteUpcomingMatchFromFirestore(match.id)
                    Toast.makeText(this@TeamProfileActivity, "Match deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
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
            "App Owner" to cbAppOwner, "App Developer" to cbAppDeveloper, "Manager" to cbManager,
            "Secretary" to cbSecretary, "Captain" to cbCaptain, "Vice Captain" to cbViceCaptain,
            "Player" to cbPlayer, "Finance Maintenance" to cbFinanceMaintenance, "Finance Contributor" to cbFinanceContributor
        )

        val preferences = listOf("Batsman", "Bowler", "All Rounder", "Wicket Keeper")
        val prefAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, preferences)
        prefAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        preferenceSpinner.adapter = prefAdapter

        if (profile != null) {
            dialogTitle.text = getString(R.string.edit)
            nameEditText.setText(profile.name)
            emailEditText.setText(profile.email)
            emailEditText.isEnabled = false
            mobileEditText.setText(profile.mobileNumber)
            alternateMobileEditText.setText(profile.alternateMobileNumber)
            val prefIndex = preferences.indexOf(profile.playerPreference)
            if (prefIndex >= 0) preferenceSpinner.setSelection(prefIndex)
            val current = profile.additionalResponsibility?.split(",")?.map { it.trim() } ?: emptyList()
            checkboxMap.forEach { (name, cb) -> cb.isChecked = current.contains(name) }
        } else {
            dialogTitle.text = "Add User"
            cbPlayer.isChecked = true
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val email = emailEditText.text.toString().trim()
                if (email.isEmpty()) return@setPositiveButton
                val selected = checkboxMap.filter { it.value.isChecked }.keys.joinToString(", ")
                val newProfile = UserProfile(email = email, name = nameEditText.text.toString().trim(), playerPreference = preferenceSpinner.selectedItem.toString(), mobileNumber = mobileEditText.text.toString().trim(), alternateMobileNumber = alternateMobileEditText.text.toString().trim(), additionalResponsibility = selected)
                lifecycleScope.launch {
                    userProfileRepository.insertOrUpdate(newProfile)
                    syncService.syncUserProfile(newProfile)
                    loadData()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteUserDialog(profile: UserProfile) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete))
            .setMessage("Are you sure?")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lifecycleScope.launch {
                    firestoreRepository.deleteUserProfile(profile.email)
                    userProfileRepository.delete(profile.email)
                    loadData()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun loadPlayerAvailability(checkedId: Int = availabilityFilterGroup.checkedButtonId) {
        val matchDate = allMatches.firstOrNull()?.dateUtcMillis ?: return
        lifecycleScope.launch {
            try {
                val confirmedAvailabilities = firestoreRepository.downloadAllMatchAvailabilities(matchDate, null)
                val confirmedMap = confirmedAvailabilities.associateBy { it.userEmail }
                val allPlayersAvailability = allUserProfiles.map { profile ->
                    confirmedMap[profile.email] ?: FirestoreMatchAvailability(userEmail = profile.email, matchDate = matchDate, lastModified = 0L)
                }
                val filteredList = when (checkedId) {
                    R.id.filterAvailable -> allPlayersAvailability.filter { it.lastModified != 0L && it.available }
                    R.id.filterNotAvailable -> allPlayersAvailability.filter { it.lastModified != 0L && !it.available }
                    else -> allPlayersAvailability
                }
                val namesMap = allUserProfiles.associate { it.email to (it.name ?: it.email) }
                playerAvailabilityAdapter = PlayerAvailabilityAdapter(namesMap) { /* update logic */ }
                availabilityRecyclerView.adapter = playerAvailabilityAdapter
                playerAvailabilityAdapter.submitList(filteredList)
                emptyAvailabilityTextView.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {}
        }
    }

    private fun exportAvailabilityReport() {
        val matchDate = allMatches.firstOrNull()?.dateUtcMillis ?: return
        lifecycleScope.launch {
            try {
                val confirmedAvailabilities = firestoreRepository.downloadAllMatchAvailabilities(matchDate, null)
                val confirmedMap = confirmedAvailabilities.associateBy { it.userEmail }
                val allPlayersAvailability = allUserProfiles.map { profile ->
                    confirmedMap[profile.email] ?: FirestoreMatchAvailability(userEmail = profile.email, matchDate = matchDate, lastModified = 0L)
                }
                generateAvailabilityPdf(allPlayersAvailability, allMatches.firstOrNull())
            } catch (e: Exception) {}
        }
    }

    private fun generateAvailabilityPdf(availabilities: List<FirestoreMatchAvailability>, match: UpcomingMatch?) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 18f }
        val headerPaint = Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 12f }
        val textPaint = Paint().apply { textSize = 10f }
        var yPos = 40f
        canvas.drawText("Magnum Cricket Club - Player Availability", 40f, yPos, titlePaint)
        yPos += 30f
        if (match != null) {
            val fmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
            canvas.drawText("Match: ${match.team1} vs ${match.team2}", 40f, yPos, headerPaint)
            yPos += 20f
            canvas.drawText("Date: ${fmt.format(Date(match.dateUtcMillis))}", 40f, yPos, headerPaint)
            yPos += 20f
        }
        yPos += 30f
        for (item in availabilities) {
            val name = allUserProfiles.find { it.email == item.userEmail }?.name ?: item.userEmail
            val status = if (item.lastModified == 0L) "Pending" else if (item.available) "Available" else "Not Available"
            canvas.drawText("$name: $status", 40f, yPos, textPaint)
            yPos += 20f
            if (yPos > 800) break
        }
        pdfDocument.finishPage(page)
        val fileName = "Availability_Report_${System.currentTimeMillis()}.pdf"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, fileName); put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf"); put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let { contentResolver.openOutputStream(it)?.use { pdfDocument.writeTo(it) } }
            } else {
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                pdfDocument.writeTo(FileOutputStream(file))
            }
            Toast.makeText(this, "Report saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {} finally { pdfDocument.close() }
    }
}
