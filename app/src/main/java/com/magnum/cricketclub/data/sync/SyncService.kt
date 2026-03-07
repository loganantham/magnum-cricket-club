package com.magnum.cricketclub.data.sync

import android.content.Context
import com.magnum.cricketclub.data.*
import com.magnum.cricketclub.data.remote.FirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SyncService(private val context: Context) {
    private val firestoreRepo = FirestoreRepository()
    private val database = AppDatabase.getDatabase(context)
    private val expenseDao = database.expenseDao()
    private val expenseTypeDao = database.expenseTypeDao()
    private val incomeTypeDao = database.incomeTypeDao()
    private val appConfigDao = database.appConfigDao()
    private val contributionLedgerDao = database.contributionLedgerDao()
    private val userProfileDao = database.userProfileDao()
    private val upcomingMatchDao = database.upcomingMatchDao()

    enum class SyncStatus {
        IDLE, SYNCING, SUCCESS, ERROR
    }

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /**
     * Full sync: Downloads all data from Firestore and updates local database
     */
    suspend fun syncFromFirestore() {
        if (!firestoreRepo.isUserSignedIn()) {
            _syncStatus.value = SyncStatus.ERROR
            return
        }

        _syncStatus.value = SyncStatus.SYNCING

        try {
            withContext(Dispatchers.IO) {
                // Download expenses
                val remoteExpenses = firestoreRepo.downloadExpenses()
                val localExpenses = expenseDao.getAllExpenses().first()

                for (remoteExpense in remoteExpenses) {
                    val localExpense = localExpenses.find { it.id == remoteExpense.id }
                    if (localExpense == null) {
                        expenseDao.insertExpense(remoteExpense)
                    } else {
                        expenseDao.updateExpense(remoteExpense)
                    }
                }

                // Download expense types
                val remoteExpenseTypes = firestoreRepo.downloadExpenseTypes()
                for (type in remoteExpenseTypes) {
                    val existing = expenseTypeDao.getExpenseTypeById(type.id)
                    if (existing == null) {
                        expenseTypeDao.insertExpenseType(type)
                    } else {
                        expenseTypeDao.updateExpenseType(type)
                    }
                }

                // Download income types
                val remoteIncomeTypes = firestoreRepo.downloadIncomeTypes()
                for (type in remoteIncomeTypes) {
                    val existing = incomeTypeDao.getIncomeTypeById(type.id)
                    if (existing == null) {
                        incomeTypeDao.insertIncomeType(type)
                    } else {
                        incomeTypeDao.updateIncomeType(type)
                    }
                }

                // Download App Config
                val remoteConfigs = firestoreRepo.downloadAllAppConfigs()
                for (config in remoteConfigs) {
                    appConfigDao.insertOrUpdateConfig(config)
                }

                // Download Contribution Ledger
                val remoteLedgerEntries = firestoreRepo.downloadAllContributionLedgerEntries()
                for (entry in remoteLedgerEntries) {
                    contributionLedgerDao.insertOrReplace(entry)
                }
                
                // Download User Profiles
                val remoteProfiles = firestoreRepo.downloadAllUserProfiles()
                for (profile in remoteProfiles) {
                    userProfileDao.insert(profile)
                }

                // Download Upcoming Matches
                val remoteMatches = firestoreRepo.downloadUpcomingMatches()
                val localMatches = upcomingMatchDao.getAllMatches().first()
                
                // Delete local matches that are not in remote or are marked as deleted
                for (localMatch in localMatches) {
                    val remoteMatch = remoteMatches.find { it.id == localMatch.id }
                    if (remoteMatch == null) {
                        upcomingMatchDao.deleteMatch(localMatch)
                    }
                }

                for (remoteMatch in remoteMatches) {
                    val localMatch = localMatches.find { it.id == remoteMatch.id }
                    if (localMatch == null) {
                        upcomingMatchDao.insertMatch(remoteMatch)
                    } else {
                        upcomingMatchDao.updateMatch(remoteMatch)
                    }
                }

                _syncStatus.value = SyncStatus.SUCCESS
            }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            e.printStackTrace()
        }
    }

    /**
     * Upload local changes to Firestore
     */
    suspend fun syncToFirestore() {
        if (!firestoreRepo.isUserSignedIn()) {
            _syncStatus.value = SyncStatus.ERROR
            return
        }

        _syncStatus.value = SyncStatus.SYNCING

        try {
            withContext(Dispatchers.IO) {
                // Upload expenses
                val localExpenses = expenseDao.getAllExpenses().first()
                for (expense in localExpenses) {
                    firestoreRepo.uploadExpense(expense)
                }

                // Upload expense types
                val localExpenseTypes = expenseTypeDao.getAllExpenseTypes().first()
                for (type in localExpenseTypes) {
                    firestoreRepo.uploadExpenseType(type)
                }

                // Upload income types
                val localIncomeTypes = incomeTypeDao.getAllIncomeTypes().first()
                for (type in localIncomeTypes) {
                    firestoreRepo.uploadIncomeType(type)
                }
                
                // Upload User Profiles
                val localProfiles = userProfileDao.getAllUserProfiles()
                for (profile in localProfiles) {
                    firestoreRepo.uploadUserProfile(profile)
                }

                // Upload Upcoming Matches
                val localMatches = upcomingMatchDao.getAllMatches().first()
                for (match in localMatches) {
                    firestoreRepo.uploadUpcomingMatch(match)
                }

                _syncStatus.value = SyncStatus.SUCCESS
            }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            e.printStackTrace()
        }
    }

    /**
     * Bidirectional sync: Downloads remote changes and uploads local changes
     */
    suspend fun fullSync() {
        syncFromFirestore()
        syncToFirestore()
    }

    /**
     * Upload a single expense when it's created/updated locally
     */
    suspend fun syncExpense(expense: Expense) {
        if (firestoreRepo.isUserSignedIn()) {
            try {
                firestoreRepo.uploadExpense(expense)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Upload a single expense type when it's created/updated locally
     */
    suspend fun syncExpenseType(expenseType: ExpenseType) {
        if (firestoreRepo.isUserSignedIn()) {
            try {
                firestoreRepo.uploadExpenseType(expenseType)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Upload a single income type when it's created/updated locally
     */
    suspend fun syncIncomeType(incomeType: IncomeType) {
        if (firestoreRepo.isUserSignedIn()) {
            try {
                firestoreRepo.uploadIncomeType(incomeType)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Upload app config
     */
    suspend fun syncAppConfig(config: AppConfig) {
        if (firestoreRepo.isUserSignedIn()) {
            try {
                firestoreRepo.uploadAppConfig(config)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Upload contribution ledger entry
     */
    suspend fun syncContributionLedgerEntry(entry: ContributionLedgerEntry) {
        if (firestoreRepo.isUserSignedIn()) {
            try {
                firestoreRepo.uploadContributionLedgerEntry(entry)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Upload user profile
     */
    suspend fun syncUserProfile(userProfile: UserProfile) {
        if (firestoreRepo.isUserSignedIn()) {
            try {
                firestoreRepo.uploadUserProfile(userProfile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Upload a single upcoming match
     */
    suspend fun syncUpcomingMatch(match: UpcomingMatch) {
        if (firestoreRepo.isUserSignedIn()) {
            _syncStatus.value = SyncStatus.SYNCING
            try {
                firestoreRepo.uploadUpcomingMatch(match)
                _syncStatus.value = SyncStatus.SUCCESS
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                e.printStackTrace()
            }
        }
    }

    /**
     * Delete expense from Firestore
     */
    suspend fun deleteExpenseFromFirestore(expenseId: Long) {
        if (firestoreRepo.isUserSignedIn()) {
            try {
                firestoreRepo.deleteExpense(expenseId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Delete expense type from Firestore
     */
    suspend fun deleteExpenseTypeFromFirestore(typeId: Long) {
        if (firestoreRepo.isUserSignedIn()) {
            try {
                firestoreRepo.deleteExpenseType(typeId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Delete income type from Firestore
     */
    suspend fun deleteIncomeTypeFromFirestore(typeId: Long) {
        if (firestoreRepo.isUserSignedIn()) {
            try {
                firestoreRepo.deleteIncomeType(typeId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Delete upcoming match from Firestore
     */
    suspend fun deleteUpcomingMatchFromFirestore(matchId: Long) {
        if (firestoreRepo.isUserSignedIn()) {
            _syncStatus.value = SyncStatus.SYNCING
            try {
                firestoreRepo.deleteUpcomingMatch(matchId)
                _syncStatus.value = SyncStatus.SUCCESS
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                e.printStackTrace()
            }
        }
    }
}
