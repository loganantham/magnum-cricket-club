package com.magnum.cricketclub.ui.me

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.magnum.cricketclub.ui.BaseActivity
import android.graphics.Color
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.UserProfile
import com.magnum.cricketclub.data.UserProfileRepository
import com.magnum.cricketclub.data.remote.FirestoreRepository
import com.magnum.cricketclub.data.sync.SyncService
import com.magnum.cricketclub.ui.auth.AuthActivity
import com.magnum.cricketclub.utils.SuccessOverlay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MeActivity : BaseActivity() {
    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var newPasswordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var changePasswordButton: MaterialButton
    private lateinit var roleBatsman: MaterialCardView
    private lateinit var roleBowler: MaterialCardView
    private lateinit var roleAllRounder: MaterialCardView
    private lateinit var roleWicketKeeper: MaterialCardView
    private lateinit var roleBatsmanWicketKeeper: MaterialCardView
    private lateinit var responsibilityFinanceMaintenance: MaterialCardView
    private lateinit var responsibilityFinanceContributor: MaterialCardView
    private lateinit var responsibilityManager: MaterialCardView
    private lateinit var responsibilitySecretary: MaterialCardView
    private lateinit var responsibilityCaptain: MaterialCardView
    private lateinit var responsibilityViceCaptain: MaterialCardView
    private lateinit var responsibilityPlayer: MaterialCardView
    private lateinit var mobileNumberEditText: TextInputEditText
    private lateinit var alternateMobileNumberEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var logoutButton: MaterialButton
    
    private var selectedRole: String? = null
    private var selectedResponsibilities: MutableSet<String> = mutableSetOf()
    
    private var auth: FirebaseAuth? = null
    private lateinit var userProfileRepository: UserProfileRepository
    private val firestoreRepository = FirestoreRepository()
    private lateinit var syncService: SyncService
    private var currentEmail: String = ""
    private var editUserEmail: String? = null
    private var currentUserProfile: UserProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_me)
        
        // Check if we are editing another user (Admin only)
        editUserEmail = intent.getStringExtra("edit_user_email")
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = if (editUserEmail != null) "Edit Player" else getString(R.string.me)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize Firebase Auth
        auth = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
        
        userProfileRepository = UserProfileRepository(application)
        syncService = SyncService(applicationContext)
        
        // Get current user email
        currentEmail = auth?.currentUser?.email ?: ""
        if (currentEmail.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        initializeViews()
        loadUserProfile()
        setupClickListeners()
        
        // Setup bottom navigation
        setupBottomNavigation()
    }
    
    private fun initializeViews() {
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        newPasswordEditText = findViewById(R.id.newPasswordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        changePasswordButton = findViewById(R.id.changePasswordButton)
        roleBatsman = findViewById(R.id.roleBatsman)
        roleBowler = findViewById(R.id.roleBowler)
        roleAllRounder = findViewById(R.id.roleAllRounder)
        roleWicketKeeper = findViewById(R.id.roleWicketKeeper)
        roleBatsmanWicketKeeper = findViewById(R.id.roleBatsmanWicketKeeper)
        responsibilityFinanceMaintenance = findViewById(R.id.responsibilityFinanceMaintenance)
        responsibilityFinanceContributor = findViewById(R.id.responsibilityFinanceContributor)
        responsibilityManager = findViewById(R.id.responsibilityManager)
        responsibilitySecretary = findViewById(R.id.responsibilitySecretary)
        responsibilityCaptain = findViewById(R.id.responsibilityCaptain)
        responsibilityViceCaptain = findViewById(R.id.responsibilityViceCaptain)
        responsibilityPlayer = findViewById(R.id.responsibilityPlayer)
        mobileNumberEditText = findViewById(R.id.mobileNumberEditText)
        alternateMobileNumberEditText = findViewById(R.id.alternateMobileNumberEditText)
        saveButton = findViewById(R.id.saveButton)
        logoutButton = findViewById(R.id.logoutButton)
        
        emailEditText.setText(editUserEmail ?: currentEmail)

        // Hide password change and logout if editing another user
        if (editUserEmail != null) {
            val pwdContainer = newPasswordEditText.parent?.parent as? ViewGroup
            pwdContainer?.visibility = View.GONE
            logoutButton.visibility = View.GONE
        }
    }

    private fun updateAdminUI() {
        // Only allow editing responsibility if the current user is an admin
        if (currentUserProfile?.isAdmin() == true) {
            enableResponsibilityEditing()
            val responsibilitySelectionContainer = findViewById<android.view.View>(R.id.responsibilitySelectionContainer)
            val parentLayout = responsibilitySelectionContainer.parent as ViewGroup
            for (i in 0 until parentLayout.childCount) {
                val child = parentLayout.getChildAt(i)
                if (child is TextView && child.text.toString().contains("read-only", ignoreCase = true)) {
                    child.text = "Select additional responsibilities"
                    break
                }
            }
        } else {
            disableResponsibilityEditing()
        }
        invalidateOptionsMenu()
    }

    private fun enableResponsibilityEditing() {
        val cards = listOf(
            responsibilityFinanceMaintenance,
            responsibilityFinanceContributor,
            responsibilityManager,
            responsibilitySecretary,
            responsibilityCaptain,
            responsibilityViceCaptain,
            responsibilityPlayer
        )
        
        cards.forEach { card ->
            card.isClickable = true
            card.isFocusable = true
            card.isEnabled = true
            
            val layout = card.getChildAt(0) as ViewGroup
            for (i in 0 until layout.childCount) {
                layout.getChildAt(i).alpha = 1.0f
            }
        }
    }

    private fun disableResponsibilityEditing() {
        val cards = listOf(
            responsibilityFinanceMaintenance,
            responsibilityFinanceContributor,
            responsibilityManager,
            responsibilitySecretary,
            responsibilityCaptain,
            responsibilityViceCaptain,
            responsibilityPlayer
        )
        
        cards.forEach { card ->
            card.isClickable = false
            card.isFocusable = false
            card.isEnabled = false
        }
    }
    
    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                // First, load the current logged-in user's profile to check for admin status
                currentUserProfile = userProfileRepository.getUserProfileSync(currentEmail)
                updateAdminUI()

                // Then, load the profile being viewed (self or other)
                val targetEmail = editUserEmail ?: currentEmail
                val profileToDisplay = if (targetEmail == currentEmail) currentUserProfile else userProfileRepository.getUserProfileSync(targetEmail)
                
                profileToDisplay?.let {
                    nameEditText.setText(it.name ?: "")
                    selectedRole = it.playerPreference
                    
                    selectedResponsibilities.clear()
                    it.additionalResponsibility?.split(",")?.forEach { r ->
                        val trimmed = r.trim()
                        if (trimmed.isNotEmpty()) {
                            selectedResponsibilities.add(trimmed)
                        }
                    }
                    
                    updateRoleSelection(it.playerPreference)
                    updateResponsibilitySelection()
                    mobileNumberEditText.setText(it.mobileNumber ?: "")
                    alternateMobileNumberEditText.setText(it.alternateMobileNumber ?: "")
                }
            } catch (e: Exception) {
                android.util.Log.e("MeActivity", "Error loading user profile", e)
            }
        }
    }
    
    private fun setupClickListeners() {
        roleBatsman.setOnClickListener { selectRole("Batsman") }
        roleBowler.setOnClickListener { selectRole("Bowler") }
        roleAllRounder.setOnClickListener { selectRole("All Rounder") }
        roleWicketKeeper.setOnClickListener { selectRole("Wicket Keeper") }
        roleBatsmanWicketKeeper.setOnClickListener { selectRole("Batsman + Wicket Keeper") }

        responsibilityFinanceMaintenance.setOnClickListener { toggleResponsibility("Finance Maintenance") }
        responsibilityFinanceContributor.setOnClickListener { toggleResponsibility("Finance Contributor") }
        responsibilityManager.setOnClickListener { toggleResponsibility("Manager") }
        responsibilitySecretary.setOnClickListener { toggleResponsibility("Secretary") }
        responsibilityCaptain.setOnClickListener { toggleResponsibility("Captain") }
        responsibilityViceCaptain.setOnClickListener { toggleResponsibility("Vice Captain") }
        responsibilityPlayer.setOnClickListener { toggleResponsibility("Player") }
        
        changePasswordButton.setOnClickListener { changePassword() }
        
        saveButton.setOnClickListener { saveProfile() }

        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        auth?.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun selectRole(role: String) {
        selectedRole = role
        updateRoleSelection(role)
    }

    private fun toggleResponsibility(responsibility: String) {
        if (selectedResponsibilities.contains(responsibility)) {
            selectedResponsibilities.remove(responsibility)
        } else {
            selectedResponsibilities.add(responsibility)
        }
        updateResponsibilitySelection()
    }
    
    private fun updateRoleSelection(role: String?) {
        resetRoleCards()
        when (role) {
            "Batsman" -> highlightRoleCard(roleBatsman)
            "Bowler" -> highlightRoleCard(roleBowler)
            "All Rounder" -> highlightRoleCard(roleAllRounder)
            "Wicket Keeper" -> highlightRoleCard(roleWicketKeeper)
            "Batsman + Wicket Keeper" -> highlightRoleCard(roleBatsmanWicketKeeper)
        }
    }

    private fun highlightRoleCard(card: MaterialCardView) {
        card.strokeWidth = 4
        card.strokeColor = getColor(R.color.primary_color)
        card.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
    }
    
    private fun resetRoleCards() {
        val cards = listOf(roleBatsman, roleBowler, roleAllRounder, roleWicketKeeper, roleBatsmanWicketKeeper)
        cards.forEach { card ->
            card.strokeWidth = 0
            card.setCardBackgroundColor(Color.WHITE)
        }
    }
    
    private fun updateResponsibilitySelection() {
        resetResponsibilityCards()
        selectedResponsibilities.forEach { responsibility ->
            val card = when (responsibility) {
                "Finance Maintenance" -> responsibilityFinanceMaintenance
                "Finance Contributor" -> responsibilityFinanceContributor
                "Manager" -> responsibilityManager
                "Secretary" -> responsibilitySecretary
                "Captain" -> responsibilityCaptain
                "Vice Captain" -> responsibilityViceCaptain
                "Player" -> responsibilityPlayer
                else -> null
            }

            card?.let { c ->
                c.strokeWidth = 4
                c.strokeColor = getColor(R.color.primary_color)
                c.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                val layout = c.getChildAt(0) as ViewGroup
                for (i in 0 until layout.childCount) {
                    layout.getChildAt(i).alpha = 1.0f
                }
            }
        }
    }
    
    private fun resetResponsibilityCards() {
        val cards = listOf(
            responsibilityFinanceMaintenance,
            responsibilityFinanceContributor,
            responsibilityManager,
            responsibilitySecretary,
            responsibilityCaptain,
            responsibilityViceCaptain,
            responsibilityPlayer
        )
        
        val isAdmin = currentUserProfile?.isAdmin() == true
        
        cards.forEach { card ->
            card.strokeWidth = 0
            card.setCardBackgroundColor(Color.WHITE)
            val layout = card.getChildAt(0) as ViewGroup
            val targetAlpha = if (isAdmin) 1.0f else 0.5f
            for (i in 0 until layout.childCount) {
                layout.getChildAt(i).alpha = targetAlpha
            }
        }
    }
    
    private fun changePassword() {
        val newPassword = newPasswordEditText.text?.toString() ?: ""
        val confirmPassword = confirmPasswordEditText.text?.toString() ?: ""
        
        if (newPassword.isEmpty() || newPassword.length < 6) {
            newPasswordEditText.error = "Min 6 characters"
            newPasswordEditText.requestFocus()
            return
        }
        
        if (newPassword != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            confirmPasswordEditText.requestFocus()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setMessage("Your password will be updated and you will be logged out immediately.")
            .setPositiveButton("Continue") { _, _ -> updatePassword(newPassword) }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updatePassword(newPassword: String) {
        val user = auth?.currentUser ?: return
        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Updated. Logging out.", Toast.LENGTH_LONG).show()
                    auth?.signOut()
                    startActivity(Intent(this, AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                } else {
                    Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun saveProfile() {
        val name = nameEditText.text?.toString()?.trim() ?: ""
        val mobileNumber = mobileNumberEditText.text?.toString()?.trim() ?: ""
        val alternateMobileNumber = alternateMobileNumberEditText.text?.toString()?.trim() ?: ""
        
        if (mobileNumber.isNotEmpty() && !isValidMobileNumber(mobileNumber)) {
            mobileNumberEditText.error = "Invalid mobile number"
            return
        }
        
        val responsibilityString = selectedResponsibilities.joinToString(",")
        
        val userProfile = UserProfile(
            email = editUserEmail ?: currentEmail,
            name = if (name.isEmpty()) null else name,
            playerPreference = selectedRole,
            mobileNumber = if (mobileNumber.isEmpty()) null else mobileNumber,
            alternateMobileNumber = if (alternateMobileNumber.isEmpty()) null else alternateMobileNumber,
            additionalResponsibility = if (responsibilityString.isEmpty()) null else responsibilityString
        )
        
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    userProfileRepository.insertOrUpdate(userProfile)
                }
                showSuccessOverlay()
                
                launch(Dispatchers.IO) {
                    try {
                        syncService.syncUserProfile(userProfile)
                    } catch (e: Exception) {
                        android.util.Log.e("MeActivity", "Firebase sync failed", e)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MeActivity, "Failed to save", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun isValidMobileNumber(number: String): Boolean = number.matches(Regex("^[0-9]{10}$"))
    
    private fun showSuccessOverlay() {
        val rootView = findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.rootCoordinatorLayout)
            ?: window?.decorView?.rootView as? ViewGroup
        
        if (rootView != null) {
            SuccessOverlay.show(rootView, "Profile saved successfully!", 2000)
        } else {
            Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
