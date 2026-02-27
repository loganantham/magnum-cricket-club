package com.magnum.cricketclub.ui.me

import android.content.Intent
import android.os.Bundle
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
import com.magnum.cricketclub.ui.auth.AuthActivity
import kotlinx.coroutines.launch

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
    private lateinit var mobileNumberEditText: TextInputEditText
    private lateinit var alternateMobileNumberEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton
    
    private var selectedRole: String? = null
    
    private var auth: FirebaseAuth? = null
    private lateinit var userProfileRepository: UserProfileRepository
    private val firestoreRepository = FirestoreRepository()
    private var currentEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_me)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.me)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize Firebase Auth
        auth = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
        
        userProfileRepository = UserProfileRepository(application)
        
        // Get current user email
        currentEmail = auth?.currentUser?.email ?: ""
        if (currentEmail.isEmpty()) {
            // If no Firebase auth, try to get from local storage or show error
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
        mobileNumberEditText = findViewById(R.id.mobileNumberEditText)
        alternateMobileNumberEditText = findViewById(R.id.alternateMobileNumberEditText)
        saveButton = findViewById(R.id.saveButton)
        
        emailEditText.setText(currentEmail)
    }
    
    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val profile = userProfileRepository.getUserProfileSync(currentEmail)
                profile?.let {
                    nameEditText.setText(it.name ?: "")
                    selectedRole = it.playerPreference
                    updateRoleSelection(it.playerPreference)
                    mobileNumberEditText.setText(it.mobileNumber ?: "")
                    alternateMobileNumberEditText.setText(it.alternateMobileNumber ?: "")
                }
            } catch (e: Exception) {
                android.util.Log.e("MeActivity", "Error loading user profile", e)
            }
        }
    }
    
    private fun setupClickListeners() {
        // Role selection cards
        roleBatsman.setOnClickListener {
            selectRole("Batsman")
        }
        
        roleBowler.setOnClickListener {
            selectRole("Bowler")
        }
        
        roleAllRounder.setOnClickListener {
            selectRole("All Rounder")
        }
        
        roleWicketKeeper.setOnClickListener {
            selectRole("Wicket Keeper")
        }
        
        roleBatsmanWicketKeeper.setOnClickListener {
            selectRole("Batsman + Wicket Keeper")
        }
        
        // Change Password button
        changePasswordButton.setOnClickListener {
            changePassword()
        }
        
        // Save button
        saveButton.setOnClickListener {
            saveProfile()
        }
    }
    
    private fun selectRole(role: String) {
        selectedRole = role
        updateRoleSelection(role)
    }
    
    private fun updateRoleSelection(role: String?) {
        // Reset all cards
        resetRoleCards()
        
        // Highlight selected card
        when (role) {
            "Batsman" -> {
                roleBatsman.strokeWidth = 4
                roleBatsman.strokeColor = getColor(R.color.primary_color)
                roleBatsman.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
            }
            "Bowler" -> {
                roleBowler.strokeWidth = 4
                roleBowler.strokeColor = getColor(R.color.primary_color)
                roleBowler.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
            }
            "All Rounder" -> {
                roleAllRounder.strokeWidth = 4
                roleAllRounder.strokeColor = getColor(R.color.primary_color)
                roleAllRounder.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
            }
            "Wicket Keeper" -> {
                roleWicketKeeper.strokeWidth = 4
                roleWicketKeeper.strokeColor = getColor(R.color.primary_color)
                roleWicketKeeper.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
            }
            "Batsman + Wicket Keeper" -> {
                roleBatsmanWicketKeeper.strokeWidth = 4
                roleBatsmanWicketKeeper.strokeColor = getColor(R.color.primary_color)
                roleBatsmanWicketKeeper.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
            }
        }
    }
    
    private fun resetRoleCards() {
        val cards = listOf(roleBatsman, roleBowler, roleAllRounder, roleWicketKeeper, roleBatsmanWicketKeeper)
        cards.forEach { card ->
            card.strokeWidth = 0
            card.setCardBackgroundColor(Color.WHITE)
        }
    }
    
    private fun changePassword() {
        val newPassword = newPasswordEditText.text?.toString() ?: ""
        val confirmPassword = confirmPasswordEditText.text?.toString() ?: ""
        
        // Validation
        if (newPassword.isEmpty()) {
            newPasswordEditText.error = "New password is required"
            newPasswordEditText.requestFocus()
            return
        }
        
        if (newPassword.length < 6) {
            newPasswordEditText.error = "Password must be at least 6 characters"
            newPasswordEditText.requestFocus()
            return
        }
        
        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.error = "Please confirm your password"
            confirmPasswordEditText.requestFocus()
            return
        }
        
        if (newPassword != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            confirmPasswordEditText.requestFocus()
            return
        }
        
        // Clear errors
        newPasswordEditText.error = null
        confirmPasswordEditText.error = null
        
        // Show confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setMessage("Your password will be updated and you will be logged out immediately. You will need to login again with your new password.")
            .setPositiveButton("Continue") { _, _ ->
                updatePassword(newPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updatePassword(newPassword: String) {
        val firebaseAuth = auth ?: run {
            Toast.makeText(this, "Firebase is not configured", Toast.LENGTH_SHORT).show()
            return
        }
        
        val user = firebaseAuth.currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        
        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password updated successfully. You will be logged out now.", Toast.LENGTH_LONG).show()
                    // Logout and navigate to login screen
                    firebaseAuth.signOut()
                    val intent = Intent(this, AuthActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val error = task.exception
                    val errorMessage = when {
                        error?.message?.contains("requires-recent-login") == true -> 
                            "For security reasons, please login again before changing your password."
                        error?.message?.contains("weak") == true -> 
                            "Password is too weak. Please use a stronger password."
                        else -> "Failed to update password: ${error?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    android.util.Log.e("MeActivity", "Password update failed", error)
                }
            }
    }
    
    private fun saveProfile() {
        val name = nameEditText.text?.toString()?.trim() ?: ""
        val mobileNumber = mobileNumberEditText.text?.toString()?.trim() ?: ""
        val alternateMobileNumber = alternateMobileNumberEditText.text?.toString()?.trim() ?: ""
        
        // Validate mobile numbers if provided
        if (mobileNumber.isNotEmpty() && !isValidMobileNumber(mobileNumber)) {
            mobileNumberEditText.error = "Please enter a valid mobile number"
            mobileNumberEditText.requestFocus()
            return
        }
        
        if (alternateMobileNumber.isNotEmpty() && !isValidMobileNumber(alternateMobileNumber)) {
            alternateMobileNumberEditText.error = "Please enter a valid mobile number"
            alternateMobileNumberEditText.requestFocus()
            return
        }
        
        // Clear errors
        mobileNumberEditText.error = null
        alternateMobileNumberEditText.error = null
        
        val userProfile = UserProfile(
            email = currentEmail,
            name = if (name.isEmpty()) null else name,
            playerPreference = selectedRole,
            mobileNumber = if (mobileNumber.isEmpty()) null else mobileNumber,
            alternateMobileNumber = if (alternateMobileNumber.isEmpty()) null else alternateMobileNumber
        )
        
        lifecycleScope.launch {
            try {
                // Save to local database
                userProfileRepository.insertOrUpdate(userProfile)
                
                // Sync to Firebase
                try {
                    firestoreRepository.uploadUserProfile(userProfile)
                    android.util.Log.d("MeActivity", "Profile synced to Firebase successfully")
                } catch (e: Exception) {
                    android.util.Log.e("MeActivity", "Error syncing profile to Firebase", e)
                    // Don't show error to user, local save was successful
                }
                
                Toast.makeText(this@MeActivity, "Profile saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("MeActivity", "Error saving profile", e)
                Toast.makeText(this@MeActivity, "Failed to save profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun isValidMobileNumber(number: String): Boolean {
        // Basic validation: 10 digits (can be extended for international format)
        return number.matches(Regex("^[0-9]{10}$"))
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
