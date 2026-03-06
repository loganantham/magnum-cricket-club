package com.magnum.cricketclub.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.AppDatabase
import com.magnum.cricketclub.data.AppConfigRepository
import com.magnum.cricketclub.data.sync.SyncService
import com.magnum.cricketclub.ui.home.HomeActivity
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {
    private var auth: FirebaseAuth? = null
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var signInButton: MaterialButton
    private lateinit var signUpButton: MaterialButton
    
    private val configRepository by lazy { AppConfigRepository(AppDatabase.getDatabase(application).appConfigDao()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        auth = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            android.util.Log.d("AuthActivity", "Firebase not configured, skipping authentication")
            navigateToHome()
            return
        }

        val mode = intent.getStringExtra("mode")
        val isAddingUserMode = mode == "signup"

        if (!isAddingUserMode && auth?.currentUser != null) {
            navigateToHome()
            return
        }

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        signInButton = findViewById(R.id.signInButton)
        signUpButton = findViewById(R.id.signUpButton)
        findViewById<MaterialButton>(R.id.googleSignInButton).visibility = android.view.View.GONE
        findViewById<android.widget.TextView>(R.id.dividerTextView).visibility = android.view.View.GONE

        if (isAddingUserMode) {
            supportActionBar?.title = "Add New User"
            signInButton.visibility = android.view.View.GONE
            signUpButton.text = "Create User"
        }

        signInButton.setOnClickListener {
            signIn()
        }

        signUpButton.setOnClickListener {
            signUp()
        }
    }

    private fun isEmailDomainAllowed(email: String, allowedDomain: String?): Boolean {
        if (allowedDomain.isNullOrBlank()) return true
        val domain = email.substringAfterLast("@", "").lowercase()
        return domain == allowedDomain.lowercase()
    }

    private fun signIn() {
        val email = emailEditText.text?.toString()?.trim() ?: ""
        val password = passwordEditText.text?.toString()?.trim() ?: ""

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and Password are required", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val allowedDomain = configRepository.getConfigValue(AppConfigRepository.KEY_ALLOWED_SIGNUP_DOMAIN)
            if (!isEmailDomainAllowed(email, allowedDomain)) {
                Toast.makeText(this@AuthActivity, "Sign in restricted", Toast.LENGTH_LONG).show()
                return@launch
            }

            auth?.signInWithEmailAndPassword(email, password)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this@AuthActivity, "Signed in successfully", Toast.LENGTH_SHORT).show()
                        lifecycleScope.launch {
                            SyncService(this@AuthActivity).syncFromFirestore()
                        }
                        navigateToHome()
                    } else {
                        Toast.makeText(this@AuthActivity, "Sign in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun signUp() {
        val email = emailEditText.text?.toString()?.trim() ?: ""
        val password = passwordEditText.text?.toString()?.trim() ?: ""

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and Password are required", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val allowedDomain = configRepository.getConfigValue(AppConfigRepository.KEY_ALLOWED_SIGNUP_DOMAIN)
            if (!isEmailDomainAllowed(email, allowedDomain)) {
                Toast.makeText(this@AuthActivity, "Sign up restricted", Toast.LENGTH_LONG).show()
                return@launch
            }

            auth?.createUserWithEmailAndPassword(email, password)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this@AuthActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        if (intent.getStringExtra("mode") == "signup") {
                            finish()
                        } else {
                            navigateToHome()
                        }
                    } else {
                        Toast.makeText(this@AuthActivity, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
