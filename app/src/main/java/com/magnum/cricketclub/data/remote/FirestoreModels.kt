package com.magnum.cricketclub.data.remote

import com.google.firebase.firestore.PropertyName

/**
 * Firestore models for syncing with cloud
 * These models match the Room entities but are optimized for Firestore
 */

data class FirestoreExpense(
    @PropertyName("id") val id: Long = 0,
    @PropertyName("expenseTypeId") val expenseTypeId: Long? = null,
    @PropertyName("incomeTypeId") val incomeTypeId: Long? = null,
    @PropertyName("amount") val amount: Double = 0.0,
    @PropertyName("description") val description: String = "",
    @PropertyName("date") val date: Long = System.currentTimeMillis(),
    @PropertyName("isIncome") val isIncome: Boolean = false,
    @PropertyName("userId") val userId: String = "",
    @PropertyName("teamId") val teamId: String = "",
    @PropertyName("lastModified") val lastModified: Long = System.currentTimeMillis(),
    @PropertyName("isDeleted") val isDeleted: Boolean = false
)

data class FirestoreExpenseType(
    @PropertyName("id") val id: Long = 0,
    @PropertyName("name") val name: String = "",
    @PropertyName("description") val description: String = "",
    @PropertyName("teamId") val teamId: String = "",
    @PropertyName("lastModified") val lastModified: Long = System.currentTimeMillis(),
    @PropertyName("isDeleted") val isDeleted: Boolean = false
)

data class FirestoreIncomeType(
    @PropertyName("id") val id: Long = 0,
    @PropertyName("name") val name: String = "",
    @PropertyName("description") val description: String = "",
    @PropertyName("teamId") val teamId: String = "",
    @PropertyName("lastModified") val lastModified: Long = System.currentTimeMillis(),
    @PropertyName("isDeleted") val isDeleted: Boolean = false
)

data class FirestoreTeam(
    @PropertyName("id") val id: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("members") val members: List<String> = emptyList(),
    @PropertyName("createdBy") val createdBy: String = "",
    @PropertyName("createdAt") val createdAt: Long = System.currentTimeMillis()
)

data class FirestoreUserProfile(
    @PropertyName("email") val email: String = "",
    @PropertyName("name") val name: String? = null,
    @PropertyName("playerPreference") val playerPreference: String? = null,
    @PropertyName("mobileNumber") val mobileNumber: String? = null,
    @PropertyName("alternateMobileNumber") val alternateMobileNumber: String? = null,
    @PropertyName("additionalResponsibility") val additionalResponsibility: String? = null,
    @PropertyName("userId") val userId: String = "",
    @PropertyName("teamId") val teamId: String = "",
    @PropertyName("lastModified") val lastModified: Long = System.currentTimeMillis()
)

data class FirestoreUpcomingMatch(
    @PropertyName("dateUtcMillis") val dateUtcMillis: Long = 0,
    @PropertyName("team1") val team1: String = "",
    @PropertyName("team2") val team2: String = "",
    @PropertyName("groundName") val groundName: String = "",
    @PropertyName("groundLocation") val groundLocation: String = "",
    @PropertyName("groundFees") val groundFees: Double = 0.0,
    @PropertyName("ballProvided") val ballProvided: Boolean = false,
    @PropertyName("noOfBalls") val noOfBalls: Int = 0,
    @PropertyName("ballName") val ballName: String? = null,
    @PropertyName("overs") val overs: Int = 20,
    @PropertyName("teamId") val teamId: String = "",
    @PropertyName("lastModified") val lastModified: Long = System.currentTimeMillis()
)

data class FirestoreMatchAvailability(
    @PropertyName("userEmail") val userEmail: String = "",
    @PropertyName("available") val available: Boolean = false,
    @PropertyName("reason") val reason: String? = null,
    @PropertyName("teamId") val teamId: String = "",
    @PropertyName("matchDate") val matchDate: Long = 0,
    @PropertyName("lastModified") val lastModified: Long = System.currentTimeMillis()
)
