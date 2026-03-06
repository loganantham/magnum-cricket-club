package com.magnum.cricketclub.ui.incometype

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.magnum.cricketclub.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class IncomeTypeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val incomeTypeRepository = IncomeTypeRepository(database.incomeTypeDao())
    private val syncService = com.magnum.cricketclub.data.sync.SyncService(application)
    
    val allIncomeTypes: Flow<List<IncomeType>> = incomeTypeRepository.getAllIncomeTypes()
    
    fun insertIncomeType(incomeType: IncomeType) {
        viewModelScope.launch {
            val id = incomeTypeRepository.insertIncomeType(incomeType)
            // Sync to Firestore with the generated ID
            syncService.syncIncomeType(incomeType.copy(id = id))
        }
    }
    
    fun updateIncomeType(incomeType: IncomeType) {
        viewModelScope.launch {
            incomeTypeRepository.updateIncomeType(incomeType)
            // Sync to Firestore
            syncService.syncIncomeType(incomeType)
        }
    }
    
    fun deleteIncomeType(incomeType: IncomeType) {
        viewModelScope.launch {
            incomeTypeRepository.deleteIncomeType(incomeType)
            // Delete from Firestore
            syncService.deleteIncomeTypeFromFirestore(incomeType.id)
        }
    }
    
    suspend fun getIncomeTypeById(id: Long): IncomeType? {
        return incomeTypeRepository.getIncomeTypeById(id)
    }
}
