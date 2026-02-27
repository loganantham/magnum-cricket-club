package com.magnum.cricketclub.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()
    fun getAllExpensesOnly(): Flow<List<Expense>> = expenseDao.getAllExpensesOnly()
    fun getAllIncomes(): Flow<List<Expense>> = expenseDao.getAllIncomes()
    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)
    fun getTotalBalance(): Flow<Double> = expenseDao.getTotalBalance()
    fun getExpensesByType(typeId: Long): Flow<List<Expense>> = expenseDao.getExpensesByType(typeId)
    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)
    suspend fun deleteExpenseById(id: Long) = expenseDao.deleteExpenseById(id)
}
