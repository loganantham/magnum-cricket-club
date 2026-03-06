package com.magnum.cricketclub.ui.config

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.magnum.cricketclub.R
import com.magnum.cricketclub.utils.SuccessOverlay
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import android.view.ViewGroup
import kotlinx.coroutines.launch

class ConfigActivity : AppCompatActivity() {
    private lateinit var viewModel: ConfigViewModel
    private lateinit var whatsappGroupIdEditText: TextInputEditText
    private lateinit var whatsappEnabledSwitch: SwitchMaterial
    private lateinit var teamNameEditText: TextInputEditText
    private lateinit var allowedDomainEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)
        
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.configuration)

        viewModel = ViewModelProvider(this)[ConfigViewModel::class.java]

        whatsappGroupIdEditText = findViewById(R.id.whatsappGroupIdEditText)
        whatsappEnabledSwitch = findViewById(R.id.whatsappEnabledSwitch)
        teamNameEditText = findViewById(R.id.teamNameEditText)
        allowedDomainEditText = findViewById(R.id.allowedDomainEditText)
        saveButton = findViewById(R.id.saveConfigButton)

        // Load current settings
        lifecycleScope.launch {
            val groupId = viewModel.getWhatsAppGroupId()
            val enabled = viewModel.isWhatsAppEnabled()
            val teamName = viewModel.getTeamName()
            val allowedDomain = viewModel.getAllowedSignupDomain()

            groupId?.let { whatsappGroupIdEditText.setText(it) }
            whatsappEnabledSwitch.isChecked = enabled
            teamName?.let { teamNameEditText.setText(it) }
            allowedDomain?.let { allowedDomainEditText.setText(it) }
        }

        saveButton.setOnClickListener {
            saveConfig()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun saveConfig() {
        val groupId = whatsappGroupIdEditText.text.toString().trim()
        val enabled = whatsappEnabledSwitch.isChecked
        val teamName = teamNameEditText.text.toString().trim()
        val allowedDomain = allowedDomainEditText.text.toString().trim().lowercase()

        // Validate team name if provided
        if (teamName.isNotEmpty() && teamName.length < 2) {
            teamNameEditText.error = "Team name must be at least 2 characters"
            teamNameEditText.requestFocus()
            return
        }

        // Clear any previous errors
        teamNameEditText.error = null

        lifecycleScope.launch {
            viewModel.setWhatsAppGroupId(groupId)
            viewModel.setWhatsAppEnabled(enabled)
            viewModel.setTeamName(teamName)
            viewModel.setAllowedSignupDomain(allowedDomain)
            
            // Show success overlay
            val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
            SuccessOverlay.show(
                parentView = rootView,
                message = "Settings saved successfully!",
                duration = 2000,
                onDismiss = {
                    finish()
                }
            )
        }
    }
}
