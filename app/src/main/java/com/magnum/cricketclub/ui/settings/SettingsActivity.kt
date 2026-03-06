package com.magnum.cricketclub.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.AppConfigRepository
import com.magnum.cricketclub.data.AppDatabase
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var whatsappGroupIdEditText: EditText
    private lateinit var whatsappHintTextView: TextView
    private lateinit var whatsappSpacer: android.view.View
    private lateinit var whatsappNotificationsSwitch: SwitchMaterial
    private lateinit var teamNameEditText: EditText
    private lateinit var teamHintTextView: TextView
    private lateinit var teamSpacer: android.view.View
    private lateinit var allowedDomainEditText: EditText
    private lateinit var domainHintTextView: TextView
    private lateinit var domainSpacer: android.view.View
    private lateinit var saveButton: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences
    private val configRepository by lazy { AppConfigRepository(AppDatabase.getDatabase(application).appConfigDao()) }
    
    companion object {
        private const val PREF_WHATSAPP_GROUP_ID = "whatsapp_group_id"
        private const val PREF_WHATSAPP_NOTIFICATIONS_ENABLED = "whatsapp_notifications_enabled"
        private const val PREF_TEAM_NAME = "team_name"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        initializeViews()
        loadSettings()
        setupSaveButton()
    }
    
    private fun initializeViews() {
        whatsappGroupIdEditText = findViewById(R.id.whatsappGroupIdEditText)
        whatsappHintTextView = findViewById(R.id.whatsappHintTextView)
        whatsappSpacer = findViewById(R.id.whatsappSpacer)
        whatsappNotificationsSwitch = findViewById(R.id.whatsappNotificationsSwitch)
        teamNameEditText = findViewById(R.id.teamNameEditText)
        teamHintTextView = findViewById(R.id.teamHintTextView)
        teamSpacer = findViewById(R.id.teamSpacer)
        allowedDomainEditText = findViewById(R.id.allowedDomainEditText)
        domainHintTextView = findViewById(R.id.domainHintTextView)
        domainSpacer = findViewById(R.id.domainSpacer)
        saveButton = findViewById(R.id.saveButton)
        
        setupTextWatchers()
    }
    
    private fun setupTextWatchers() {
        // WhatsApp field focus and text change listeners
        whatsappGroupIdEditText.setOnFocusChangeListener { _, hasFocus ->
            updateWhatsAppFieldVisibility(hasFocus)
        }
        
        whatsappGroupIdEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateWhatsAppFieldVisibility(whatsappGroupIdEditText.hasFocus())
            }
        })
        
        // Team name field focus and text change listeners
        teamNameEditText.setOnFocusChangeListener { _, hasFocus ->
            updateTeamFieldVisibility(hasFocus)
        }
        
        teamNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTeamFieldVisibility(teamNameEditText.hasFocus())
            }
        })

        // Domain field focus and text change listeners
        allowedDomainEditText.setOnFocusChangeListener { _, hasFocus ->
            updateDomainFieldVisibility(hasFocus)
        }
        
        allowedDomainEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateDomainFieldVisibility(allowedDomainEditText.hasFocus())
            }
        })
    }
    
    private fun updateWhatsAppFieldVisibility(hasFocus: Boolean) {
        val isEmpty = whatsappGroupIdEditText.text.isNullOrBlank()
        if (hasFocus || !isEmpty) {
            whatsappHintTextView.visibility = TextView.GONE
            whatsappSpacer.visibility = android.view.View.GONE
            whatsappGroupIdEditText.visibility = android.view.View.VISIBLE
        } else {
            whatsappHintTextView.visibility = TextView.VISIBLE
            whatsappSpacer.visibility = android.view.View.VISIBLE
            whatsappGroupIdEditText.visibility = android.view.View.GONE
        }
    }
    
    private fun updateTeamFieldVisibility(hasFocus: Boolean) {
        val isEmpty = teamNameEditText.text.isNullOrBlank()
        if (hasFocus || !isEmpty) {
            teamHintTextView.visibility = TextView.GONE
            teamSpacer.visibility = android.view.View.GONE
            teamNameEditText.visibility = android.view.View.VISIBLE
        } else {
            teamHintTextView.visibility = TextView.VISIBLE
            teamSpacer.visibility = android.view.View.VISIBLE
            teamNameEditText.visibility = android.view.View.GONE
        }
    }

    private fun updateDomainFieldVisibility(hasFocus: Boolean) {
        val isEmpty = allowedDomainEditText.text.isNullOrBlank()
        if (hasFocus || !isEmpty) {
            domainHintTextView.visibility = TextView.GONE
            domainSpacer.visibility = android.view.View.GONE
            allowedDomainEditText.visibility = android.view.View.VISIBLE
        } else {
            domainHintTextView.visibility = TextView.VISIBLE
            domainSpacer.visibility = android.view.View.VISIBLE
            allowedDomainEditText.visibility = android.view.View.GONE
        }
    }
    
    private fun loadSettings() {
        // Load WhatsApp Group ID
        val whatsappGroupId = sharedPreferences.getString(PREF_WHATSAPP_GROUP_ID, "")
        if (!whatsappGroupId.isNullOrBlank()) {
            whatsappGroupIdEditText.setText(whatsappGroupId)
        }
        updateWhatsAppFieldVisibility(whatsappGroupIdEditText.hasFocus())
        
        // Load WhatsApp Notifications enabled state
        val whatsappNotificationsEnabled = sharedPreferences.getBoolean(PREF_WHATSAPP_NOTIFICATIONS_ENABLED, false)
        whatsappNotificationsSwitch.isChecked = whatsappNotificationsEnabled
        
        // Load Team Name
        val teamName = sharedPreferences.getString(PREF_TEAM_NAME, "")
        if (!teamName.isNullOrBlank()) {
            teamNameEditText.setText(teamName)
        }
        updateTeamFieldVisibility(teamNameEditText.hasFocus())

        // Load Domain Restriction from ConfigRepository
        lifecycleScope.launch {
            val domain = configRepository.getConfigValue(AppConfigRepository.KEY_ALLOWED_SIGNUP_DOMAIN) ?: ""
            if (domain.isNotEmpty()) {
                allowedDomainEditText.setText(domain)
            }
            updateDomainFieldVisibility(allowedDomainEditText.hasFocus())
        }
    }
    
    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            saveSettings()
        }
    }
    
    private fun saveSettings() {
        val editor = sharedPreferences.edit()
        
        // Save WhatsApp Group ID
        val whatsappGroupId = whatsappGroupIdEditText.text?.toString()?.trim() ?: ""
        editor.putString(PREF_WHATSAPP_GROUP_ID, whatsappGroupId)
        
        // Save WhatsApp Notifications enabled state
        editor.putBoolean(PREF_WHATSAPP_NOTIFICATIONS_ENABLED, whatsappNotificationsSwitch.isChecked)
        
        // Save Team Name
        val teamName = teamNameEditText.text?.toString()?.trim() ?: ""
        editor.putString(PREF_TEAM_NAME, teamName)
        
        editor.apply()

        // Save Domain Restriction to ConfigRepository
        val domain = allowedDomainEditText.text?.toString()?.trim()?.lowercase() ?: ""
        lifecycleScope.launch {
            configRepository.setConfig(AppConfigRepository.KEY_ALLOWED_SIGNUP_DOMAIN, domain)
            Toast.makeText(this@SettingsActivity, "Settings saved successfully", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
