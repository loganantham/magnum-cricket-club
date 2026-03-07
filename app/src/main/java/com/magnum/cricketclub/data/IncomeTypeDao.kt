package com.magnum.cricketclub.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeTypeDao {
    @Query("SELECT * FROM income_types ORDER BY name ASC")
    fun getAllIncomeTypes(): Flow<List<IncomeType>>
    
    @Query("SELECT * FROM income_types WHERE id = :id")
    suspend fun getIncomeTypeById(id: Long): IncomeType?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeType(incomeType: IncomeType): Long
    
    @Update
    suspend fun updateIncomeType(incomeType: IncomeType)
    
    @Delete
    suspend fun deleteIncomeType(incomeType: IncomeType)
    
    @Query("DELETE FROM income_types WHERE id = :id")
    suspend fun deleteIncomeTypeById(id: Long)
}
