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
    val additionalResponsibility: String? = null, // Finance Maintenance, Finance Contributor, Manager, Secretary, Captain, Vice Captain, Player
    val userId: String = ""
) {
    fun isAdmin(): Boolean {
        val responsibilities = additionalResponsibility?.split(",")?.map { it.trim() } ?: emptyList()
        return responsibilities.contains("App Owner") || responsibilities.contains("App Developer")
    }

    fun isManager(): Boolean {
        val responsibilities = additionalResponsibility?.split(",")?.map { it.trim() } ?: emptyList()
        return responsibilities.contains("Manager")
    }

    fun canManageMatches(): Boolean {
        return isManager() || isAdmin()
    }

    fun canManageFinance(): Boolean {
        val responsibilities = additionalResponsibility?.split(",")?.map { it.trim() } ?: emptyList()
        return responsibilities.contains("Finance Maintenance") || isAdmin()
    }

    fun isFinanceMaintenance(): Boolean {
        return canManageFinance()
    }

    fun isFinanceContributor(): Boolean {
        val responsibilities = additionalResponsibility?.split(",")?.map { it.trim() } ?: emptyList()
        return responsibilities.contains("Finance Contributor") || isFinanceMaintenance()
    }
}
