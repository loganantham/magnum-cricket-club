package com.magnum.cricketclub.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val email: String,
    val name: String? = null,
    val playerPreference: String? = null, // Batsman, Bowler, All Rounder, Wicket Keeper
    val mobileNumber: String? = null,
    val alternateMobileNumber: String? = null,
    val additionalResponsibility: String? = null // Finance Maintenance, Finance Contributor, Manager, Secretary, Captain, Vice Captain, Player
)
