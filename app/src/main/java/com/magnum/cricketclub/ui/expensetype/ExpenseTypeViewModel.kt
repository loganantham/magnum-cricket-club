package com.magnum.cricketclub.ui.expensetype

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.magnum.cricketclub.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ExpenseTypeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val expenseTypeRepository = ExpenseTypeRepository(database.expenseTypeDao())
    private val syncService = com.magnum.cricketclub.data.sync.SyncService(application)
    
    val allExpenseTypes: Flow<List<ExpenseType>> = expenseTypeRepository.getAllExpenseTypes()
    
    fun insertExpenseType(expenseType: ExpenseType) {
        viewModelScope.launch {
            expenseTypeRepository.insertExpenseType(expenseType)
            // Sync to Firestore
            syncService.syncExpenseType(expenseType)
        }
    }
    
    fun updateExpenseType(expenseType: ExpenseType) {
        viewModelScope.launch {
            expenseTypeRepository.updateExpenseType(expenseType)
            // Sync to Firestore
            syncService.syncExpenseType(expenseType)
        }
    }
    
    fun deleteExpenseType(expenseType: ExpenseType) {
        viewModelScope.launch {
            expenseTypeRepository.deleteExpenseType(expenseType)
            // Delete from Firestore
            syncService.deleteExpenseTypeFromFirestore(expenseType.id)
        }
    }
    
    suspend fun getExpenseTypeById(id: Long): ExpenseType? {
        return expenseTypeRepository.getExpenseTypeById(id)
    }
}
