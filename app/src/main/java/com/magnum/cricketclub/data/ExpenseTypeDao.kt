package com.magnum.cricketclub.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseTypeDao {
    @Query("SELECT * FROM expense_types ORDER BY name ASC")
    fun getAllExpenseTypes(): Flow<List<ExpenseType>>
    
    @Query("SELECT * FROM expense_types WHERE id = :id")
    suspend fun getExpenseTypeById(id: Long): ExpenseType?
    
    @Insert
    suspend fun insertExpenseType(expenseType: ExpenseType): Long
    
    @Update
    suspend fun updateExpenseType(expenseType: ExpenseType)
    
    @Delete
    suspend fun deleteExpenseType(expenseType: ExpenseType)
    
    @Query("DELETE FROM expense_types WHERE id = :id")
    suspend fun deleteExpenseTypeById(id: Long)
}
