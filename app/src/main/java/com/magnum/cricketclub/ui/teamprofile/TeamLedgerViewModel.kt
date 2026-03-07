package com.magnum.cricketclub.ui.teamprofile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.magnum.cricketclub.data.*
import com.magnum.cricketclub.data.remote.FirestoreRepository
import com.magnum.cricketclub.data.sync.SyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class TeamLedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val ledgerRepository = ContributionLedgerRepository(database.contributionLedgerDao())
    private val userProfileRepository = UserProfileRepository(application)
    private val configRepository = AppConfigRepository(database.appConfigDao())
    private val firestoreRepository = FirestoreRepository()
    private val syncService = SyncService(application)

    private val _contributors = MutableStateFlow<List<UserProfile>>(emptyList())
    val contributors: StateFlow<List<UserProfile>> = _contributors.asStateFlow()

    private val _ledgerEntries = MutableStateFlow<List<ContributionLedgerEntry>>(emptyList())
    val ledgerEntries: StateFlow<List<ContributionLedgerEntry>> = _ledgerEntries.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(-1) // -1 for All Months
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Download latest from Firestore first
                syncService.syncFromFirestore()
                
                // Load contributors
                val profiles = userProfileRepository.getAllUserProfiles()
                _contributors.value = profiles.filter { it.isFinanceContributor() }
                
                refreshLedger()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun refreshLedger() {
        val year = _selectedYear.value
        val allEntries = mutableListOf<ContributionLedgerEntry>()
        _contributors.value.forEach { contributor ->
            val entries = ledgerRepository.getEntriesForContributorAndYear(contributor.email, year)
            allEntries.addAll(entries)
        }
        _ledgerEntries.value = allEntries
    }

    fun setSelectedYear(year: Int) {
        _selectedYear.value = year
        viewModelScope.launch {
            refreshLedger()
        }
    }

    fun setSelectedMonth(month: Int) {
        _selectedMonth.value = month
    }

    fun updateStatus(email: String, year: Int, monthIndex: Int, status: String, pendingAmount: Double? = null) {
        viewModelScope.launch {
            val amount = pendingAmount ?: if (status == "Done") 0.0 else 1000.0
            val entry = ledgerRepository.upsertEntry(email, year, monthIndex, status, amount)
            // Sync to Firestore
            syncService.syncContributionLedgerEntry(entry)
            refreshLedger()
        }
    }

    fun getPendingSummary(year: Int): Map<String, List<Int>> {
        val summary = mutableMapOf<String, List<Int>>()
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        val maxMonth = if (year < currentYear) 11 else currentMonth

        _contributors.value.forEach { contributor ->
            val entries = _ledgerEntries.value.filter { it.contributorEmail == contributor.email && it.year == year }
            val pendingMonths = mutableListOf<Int>()
            for (m in 0..maxMonth) {
                val entry = entries.find { it.monthIndex == m }
                if (entry == null || entry.status != "Done") {
                    pendingMonths.add(m)
                }
            }
            if (pendingMonths.isNotEmpty()) {
                summary[contributor.email] = pendingMonths
            }
        }
        return summary
    }

    suspend fun getWhatsAppGroupId(): String? {
        return configRepository.getConfigValue(AppConfigRepository.KEY_WHATSAPP_GROUP_ID)
    }
}
