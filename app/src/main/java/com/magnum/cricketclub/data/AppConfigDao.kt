package com.magnum.cricketclub.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE key = :key")
    suspend fun getConfig(key: String): AppConfig?
    
    @Query("SELECT value FROM app_config WHERE key = :key")
    suspend fun getConfigValue(key: String): String?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: AppConfig)
    
    @Query("DELETE FROM app_config WHERE key = :key")
    suspend fun deleteConfig(key: String)

    @Query("SELECT * FROM app_config")
    suspend fun getAllConfigs(): List<AppConfig>
}
