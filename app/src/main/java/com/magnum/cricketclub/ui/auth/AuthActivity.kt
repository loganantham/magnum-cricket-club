package com.magnum.cricketclub.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.sync.SyncService
import com.magnum.cricketclub.ui.home.HomeActivity
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {
    private var auth: FirebaseAuth? = null
    private var googleSignInClient: GoogleSignInClient? = null
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var signInButton: MaterialButton
    private lateinit var signUpButton: MaterialButton
    private lateinit var googleSignInButton: MaterialButton
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.data == null) {
            android.util.Log.w("AuthActivity", "Google Sign-In result data is null - user may have cancelled")
            Toast.makeText(this, "Google Sign-In was cancelled", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            account?.let { 
                android.util.Log.d("AuthActivity", "Google Sign-In account received: ${account.email}")
                firebaseAuthWithGoogle(it) 
            } ?: run {
                android.util.Log.e("AuthActivity", "Google Sign-In account is null")
                Toast.makeText(this, "Google Sign-In failed: No account received", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            val errorCode = e.statusCode
            val errorMessage = when (errorCode) {
                12501 -> {
                    // DEVELOPER_ERROR - Most commonly caused by missing SHA-1 fingerprint
                    "Configuration error: Please add your app's SHA-1 fingerprint to Firebase Console. " +
                    "See GOOGLE_SIGNIN_TROUBLESHOOTING.md for instructions."
                }
                com.google.android.gms.common.api.CommonStatusCodes.NETWORK_ERROR -> {
                    "Network error. Please check your internet connection and try again."
                }
                com.google.android.gms.common.api.CommonStatusCodes.INTERNAL_ERROR -> {
                    "Internal error. Please try again later."
                }
                com.google.android.gms.common.api.CommonStatusCodes.INVALID_ACCOUNT -> {
                    "Invalid account. Please try again."
                }
                com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> {
                    "Sign-in was cancelled."
                }
                com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_FAILED -> {
                    "Sign-in failed. Please try again."
                }
                else -> {
                    "Google Sign-In failed (Error $errorCode): ${e.message}"
                }
            }
            android.util.Log.e("AuthActivity", "Google Sign-In failed with error code: $errorCode", e)
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.e("AuthActivity", "Unexpected error during Google Sign-In", e)
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        // Initialize Firebase Auth only if available
        auth = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            // Firebase not configured - skip authentication and go to home
            android.util.Log.d("AuthActivity", "Firebase not configured, skipping authentication")
            navigateToHome()
            return
        }

        // Check if user is already signed in
        if (auth?.currentUser != null) {
            navigateToHome()
            return
        }

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        signInButton = findViewById(R.id.signInButton)
        signUpButton = findViewById(R.id.signUpButton)
        googleSignInButton = findViewById(R.id.googleSignInButton)

        // Initialize Google Sign-In
        try {
            // Extract Web Client ID from google-services.json
            // The Web Client ID is in oauth_client array with client_type: 3
            val webClientId = extractWebClientIdFromGoogleServices()
            
            if (webClientId != null) {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                googleSignInClient = GoogleSignIn.getClient(this, gso)
                android.util.Log.d("AuthActivity", "Google Sign-In configured successfully")
            } else {
                // Hide Google Sign-In button if not configured
                googleSignInButton.visibility = android.view.View.GONE
                findViewById<android.widget.TextView>(R.id.dividerTextView)?.visibility = android.view.View.GONE
                android.util.Log.d("AuthActivity", "Google Sign-In not configured - Web Client ID not found in google-services.json")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthActivity", "Google Sign-In configuration error: ${e.message}", e)
            googleSignInButton.visibility = android.view.View.GONE
            findViewById<android.widget.TextView>(R.id.dividerTextView)?.visibility = android.view.View.GONE
        }

        signInButton.setOnClickListener {
            signIn()
        }

        signUpButton.setOnClickListener {
            signUp()
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signIn() {
        val email = emailEditText.text?.toString()?.trim() ?: ""
        val password = passwordEditText.text?.toString()?.trim() ?: ""

        // Validate email
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            emailEditText.requestFocus()
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Please enter a valid email address"
            emailEditText.requestFocus()
            return
        }

        // Validate password
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            return
        }

        val firebaseAuth = auth ?: run {
            Toast.makeText(this, "Firebase is not configured", Toast.LENGTH_SHORT).show()
            return
        }

        // Clear any previous errors
        emailEditText.error = null
        passwordEditText.error = null

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show()
                    // Sync data after sign in
                    lifecycleScope.launch {
                        SyncService(this@AuthActivity).syncFromFirestore()
                    }
                    navigateToHome()
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("password") == true -> "Incorrect password"
                        task.exception?.message?.contains("user") == true -> "No account found with this email"
                        else -> "Sign in failed: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signUp() {
        val email = emailEditText.text?.toString()?.trim() ?: ""
        val password = passwordEditText.text?.toString()?.trim() ?: ""

        // Validate email
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            emailEditText.requestFocus()
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Please enter a valid email address"
            emailEditText.requestFocus()
            return
        }

        // Validate password
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            return
        }

        if (password.length < 6) {
            passwordEditText.error = "Password must be at least 6 characters"
            passwordEditText.requestFocus()
            return
        }

        val firebaseAuth = auth ?: run {
            Toast.makeText(this, "Firebase is not configured", Toast.LENGTH_SHORT).show()
            return
        }

        // Clear any previous errors
        emailEditText.error = null
        passwordEditText.error = null

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created successfully! Welcome to Magnum Cricket Club", Toast.LENGTH_SHORT).show()
                    // Upload local data to Firestore after sign up
                    lifecycleScope.launch {
                        SyncService(this@AuthActivity).syncToFirestore()
                    }
                    navigateToHome()
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("already") == true -> "An account with this email already exists. Please sign in instead."
                        task.exception?.message?.contains("weak") == true -> "Password is too weak. Please use a stronger password."
                        else -> "Sign up failed: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signInWithGoogle() {
        val client = googleSignInClient ?: run {
            Toast.makeText(this, "Google Sign-In is not configured. Please configure Firebase first.", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            android.util.Log.d("AuthActivity", "Starting Google Sign-In flow")
            val signInIntent = client.signInIntent
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            android.util.Log.e("AuthActivity", "Error launching Google Sign-In", e)
            Toast.makeText(this, "Failed to start Google Sign-In: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val firebaseAuth = auth ?: run {
            Toast.makeText(this, "Firebase is not configured", Toast.LENGTH_SHORT).show()
            return
        }

        val idToken = account.idToken
        if (idToken == null) {
            Toast.makeText(this, "Google Sign-In failed: No ID token received", Toast.LENGTH_SHORT).show()
            android.util.Log.e("AuthActivity", "Google Sign-In account has no ID token")
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    android.util.Log.d("AuthActivity", "Google Sign-In successful: ${user?.email}")
                    Toast.makeText(this, "Signed in with Google successfully", Toast.LENGTH_SHORT).show()
                    // Sync data after sign in
                    lifecycleScope.launch {
                        SyncService(this@AuthActivity).syncFromFirestore()
                    }
                    navigateToHome()
                } else {
                    val error = task.exception
                    android.util.Log.e("AuthActivity", "Google authentication failed", error)
                    val errorMessage = when {
                        error?.message?.contains("network") == true -> "Network error. Please check your internet connection."
                        error?.message?.contains("invalid") == true -> "Invalid credentials. Please try again."
                        else -> "Google authentication failed: ${error?.message ?: "Unknown error"}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun extractWebClientIdFromGoogleServices(): String? {
        // The Web Client ID is extracted from app/google-services.json
        // It's in: client[0].oauth_client[] where client_type = 3 (Web client)
        // Current Web Client ID from google-services.json: "1076211377209-ride5umkasmacjqfjgh9836f1ttb9uls.apps.googleusercontent.com"
        // Note: After adding SHA-1, make sure to rebuild the app completely
        return "1076211377209-ride5umkasmacjqfjgh9836f1ttb9uls.apps.googleusercontent.com"
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
