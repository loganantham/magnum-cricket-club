package com.magnum.cricketclub.data

import kotlinx.coroutines.flow.Flow

class ExpenseTypeRepository(private val expenseTypeDao: ExpenseTypeDao) {
    fun getAllExpenseTypes(): Flow<List<ExpenseType>> = expenseTypeDao.getAllExpenseTypes()
    suspend fun getExpenseTypeById(id: Long): ExpenseType? = expenseTypeDao.getExpenseTypeById(id)
    suspend fun insertExpenseType(expenseType: ExpenseType): Long = expenseTypeDao.insertExpenseType(expenseType)
    suspend fun updateExpenseType(expenseType: ExpenseType) = expenseTypeDao.updateExpenseType(expenseType)
    suspend fun deleteExpenseType(expenseType: ExpenseType) = expenseTypeDao.deleteExpenseType(expenseType)
    suspend fun deleteExpenseTypeById(id: Long) = expenseTypeDao.deleteExpenseTypeById(id)
}
