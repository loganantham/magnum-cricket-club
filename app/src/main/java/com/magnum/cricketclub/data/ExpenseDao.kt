package com.magnum.cricketclub.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>
    
    @Query("SELECT * FROM expenses WHERE isIncome = 0 ORDER BY date DESC")
    fun getAllExpensesOnly(): Flow<List<Expense>>
    
    @Query("SELECT * FROM expenses WHERE isIncome = 1 ORDER BY date DESC")
    fun getAllIncomes(): Flow<List<Expense>>
    
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?
    
    @Query("SELECT COALESCE(SUM(CASE WHEN isIncome = 1 THEN amount ELSE -amount END), 0.0) FROM expenses")
    fun getTotalBalance(): Flow<Double>

    @Query("SELECT COALESCE(SUM(CASE WHEN isIncome = 1 THEN amount ELSE -amount END), 0.0) FROM expenses")
    suspend fun getLatestTotalBalance(): Double
    
    @Query("SELECT * FROM expenses WHERE expenseTypeId = :typeId ORDER BY date DESC")
    fun getExpensesByType(typeId: Long): Flow<List<Expense>>
    
    @Insert
    suspend fun insertExpense(expense: Expense): Long
    
    @Update
    suspend fun updateExpense(expense: Expense)
    
    @Delete
    suspend fun deleteExpense(expense: Expense)
    
    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)
}
