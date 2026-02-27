package com.magnum.cricketclub.data

import android.app.Application
import kotlinx.coroutines.flow.Flow

class UserProfileRepository(application: Application) {
    private val userProfileDao = AppDatabase.getDatabase(application).userProfileDao()
    
    fun getUserProfile(email: String): Flow<UserProfile?> {
        return userProfileDao.getUserProfile(email)
    }
    
    suspend fun getUserProfileSync(email: String): UserProfile? {
        return userProfileDao.getUserProfileSync(email)
    }
    
    suspend fun insertOrUpdate(userProfile: UserProfile) {
        val existing = userProfileDao.getUserProfileSync(userProfile.email)
        if (existing != null) {
            userProfileDao.update(userProfile)
        } else {
            userProfileDao.insert(userProfile)
        }
    }
    
    suspend fun delete(email: String) {
        userProfileDao.delete(email)
    }
    
    suspend fun getAllUserProfiles(): List<UserProfile> {
        return userProfileDao.getAllUserProfiles()
    }
}
