package com.magnum.cricketclub.data.sync

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.magnum.cricketclub.data.*
import com.magnum.cricketclub.data.remote.FirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncService(private val context: Context) {
    private val firestoreRepo = FirestoreRepository()
    private val database = AppDatabase.getDatabase(context)
    private val expenseDao = database.expenseDao()
    private val expenseTypeDao = database.expenseTypeDao()
    private val incomeTypeDao = database.incomeTypeDao()

    enum class SyncStatus {
        IDLE, SYNCING, SUCCESS, ERROR
    }

    var syncStatus: SyncStatus = SyncStatus.IDLE
        private set

    /**
     * Full sync: Downloads all data from Firestore and updates local database
     */
    suspend fun syncFromFirestore() {
        if (!firestoreRepo.isUserSignedIn()) {
            syncStatus = SyncStatus.ERROR
            return
        }

        syncStatus = SyncStatus.SYNCING

        try {
            withContext(Dispatchers.IO) {
                // Download expenses
                val remoteExpenses = firestoreRepo.downloadExpenses()
                val localExpenses = expenseDao.getAllExpenses().first()

                // Merge strategy: Remote wins (last write wins)
                for (remoteExpense in remoteExpenses) {
                    val localExpense = localExpenses.find { it.id == remoteExpense.id }
                    if (localExpense == null || remoteExpense.date > localExpense.date) {
                        if (localExpense == null) {
                            expenseDao.insertExpense(remoteExpense)
                        } else {
                            expenseDao.updateExpense(remoteExpense)
                        }
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

                syncStatus = SyncStatus.SUCCESS
            }
        } catch (e: Exception) {
            syncStatus = SyncStatus.ERROR
            e.printStackTrace()
        }
    }

    /**
     * Upload local changes to Firestore
     */
    suspend fun syncToFirestore() {
        if (!firestoreRepo.isUserSignedIn()) {
            syncStatus = SyncStatus.ERROR
            return
        }

        syncStatus = SyncStatus.SYNCING

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

                syncStatus = SyncStatus.SUCCESS
            }
        } catch (e: Exception) {
            syncStatus = SyncStatus.ERROR
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
}
