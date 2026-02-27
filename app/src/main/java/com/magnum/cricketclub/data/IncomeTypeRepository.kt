package com.magnum.cricketclub.data

import kotlinx.coroutines.flow.Flow

class IncomeTypeRepository(private val incomeTypeDao: IncomeTypeDao) {
    fun getAllIncomeTypes(): Flow<List<IncomeType>> = incomeTypeDao.getAllIncomeTypes()
    suspend fun getIncomeTypeById(id: Long): IncomeType? = incomeTypeDao.getIncomeTypeById(id)
    suspend fun insertIncomeType(incomeType: IncomeType): Long = incomeTypeDao.insertIncomeType(incomeType)
    suspend fun updateIncomeType(incomeType: IncomeType) = incomeTypeDao.updateIncomeType(incomeType)
    suspend fun deleteIncomeType(incomeType: IncomeType) = incomeTypeDao.deleteIncomeType(incomeType)
    suspend fun deleteIncomeTypeById(id: Long) = incomeTypeDao.deleteIncomeTypeById(id)
}
