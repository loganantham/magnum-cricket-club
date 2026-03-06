package com.magnum.cricketclub.ui.expense

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.magnum.cricketclub.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val expenseRepository = ExpenseRepository(database.expenseDao())
    private val expenseTypeRepository = ExpenseTypeRepository(database.expenseTypeDao())
    private val incomeTypeRepository = IncomeTypeRepository(database.incomeTypeDao())
    private val configRepository = AppConfigRepository(database.appConfigDao())
    private val syncService = com.magnum.cricketclub.data.sync.SyncService(application)
    
    val allExpenses: Flow<List<Expense>> = expenseRepository.getAllExpenses()
    val allExpensesOnly: Flow<List<Expense>> = expenseRepository.getAllExpensesOnly()
    val allIncomes: Flow<List<Expense>> = expenseRepository.getAllIncomes()
    val totalBalance: Flow<Double> = expenseRepository.getTotalBalance()
    val allExpenseTypes: Flow<List<ExpenseType>> = expenseTypeRepository.getAllExpenseTypes()
    val allIncomeTypes: Flow<List<IncomeType>> = incomeTypeRepository.getAllIncomeTypes()
    
    fun insertExpense(expense: Expense) {
        viewModelScope.launch {
            val id = expenseRepository.insertExpense(expense)
            // Sync to Firestore with the generated ID
            syncService.syncExpense(expense.copy(id = id))
        }
    }
    
    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.updateExpense(expense)
            // Sync to Firestore in background
            syncService.syncExpense(expense)
        }
    }
    
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
            // Delete from Firestore
            syncService.deleteExpenseFromFirestore(expense.id)
        }
    }
    
    suspend fun getExpenseById(id: Long): Expense? {
        return expenseRepository.getExpenseById(id)
    }
    
    suspend fun getExpenseTypeById(id: Long): ExpenseType? {
        return expenseTypeRepository.getExpenseTypeById(id)
    }
    
    suspend fun getIncomeTypeById(id: Long): IncomeType? {
        return incomeTypeRepository.getIncomeTypeById(id)
    }
    
    suspend fun getWhatsAppGroupId(): String? {
        return configRepository.getConfigValue(AppConfigRepository.KEY_WHATSAPP_GROUP_ID)
    }
    
    suspend fun isWhatsAppEnabled(): Boolean {
        val value = configRepository.getConfigValue(AppConfigRepository.KEY_WHATSAPP_ENABLED)
        return value?.toBoolean() ?: false
    }

    suspend fun getLatestTotalBalance(): Double {
        return expenseRepository.getLatestTotalBalance()
    }
}
