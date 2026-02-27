package com.magnum.cricketclub.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE email = :email LIMIT 1")
    fun getUserProfile(email: String): Flow<UserProfile?>
    
    @Query("SELECT * FROM user_profile WHERE email = :email LIMIT 1")
    suspend fun getUserProfileSync(email: String): UserProfile?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userProfile: UserProfile)
    
    @Update
    suspend fun update(userProfile: UserProfile)
    
    @Query("DELETE FROM user_profile WHERE email = :email")
    suspend fun delete(email: String)
    
    @Query("SELECT * FROM user_profile ORDER BY name ASC, email ASC")
    suspend fun getAllUserProfiles(): List<UserProfile>
}
