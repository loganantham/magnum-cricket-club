package com.magnum.cricketclub.ui.config

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.magnum.cricketclub.data.*
import kotlinx.coroutines.launch

class ConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val configRepository = AppConfigRepository(database.appConfigDao())
    
    suspend fun getWhatsAppGroupId(): String? {
        return configRepository.getConfigValue(AppConfigRepository.KEY_WHATSAPP_GROUP_ID)
    }
    
    suspend fun isWhatsAppEnabled(): Boolean {
        val value = configRepository.getConfigValue(AppConfigRepository.KEY_WHATSAPP_ENABLED)
        return value?.toBoolean() ?: false
    }
    
    suspend fun getTeamName(): String? {
        return configRepository.getConfigValue(AppConfigRepository.KEY_TEAM_NAME)
    }

    suspend fun getAllowedSignupDomain(): String? {
        return configRepository.getConfigValue(AppConfigRepository.KEY_ALLOWED_SIGNUP_DOMAIN)
    }
    
    fun setWhatsAppGroupId(groupId: String) {
        viewModelScope.launch {
            configRepository.setConfig(AppConfigRepository.KEY_WHATSAPP_GROUP_ID, groupId)
        }
    }
    
    fun setWhatsAppEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configRepository.setConfig(AppConfigRepository.KEY_WHATSAPP_ENABLED, enabled.toString())
        }
    }
    
    fun setTeamName(teamName: String) {
        viewModelScope.launch {
            configRepository.setConfig(AppConfigRepository.KEY_TEAM_NAME, teamName)
        }
    }

    fun setAllowedSignupDomain(domain: String) {
        viewModelScope.launch {
            configRepository.setConfig(AppConfigRepository.KEY_ALLOWED_SIGNUP_DOMAIN, domain)
        }
    }
}
