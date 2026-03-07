package com.magnum.cricketclub.data.remote

import com.google.firebase.firestore.PropertyName

/**
 * Firestore models for syncing with cloud
 * These models match the Room entities but are optimized for Firestore
 */

data class FirestoreExpense(
    @get:PropertyName("id") @set:PropertyName("id") var id: Long = 0,
    @get:PropertyName("expenseTypeId") @set:PropertyName("expenseTypeId") var expenseTypeId: Long? = null,
    @get:PropertyName("incomeTypeId") @set:PropertyName("incomeTypeId") var incomeTypeId: Long? = null,
    @get:PropertyName("amount") @set:PropertyName("amount") var amount: Double = 0.0,
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("date") @set:PropertyName("date") var date: Long = System.currentTimeMillis(),
    @get:PropertyName("income") @set:PropertyName("income") var isIncome: Boolean = false,
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("teamId") @set:PropertyName("teamId") var teamId: String = "",
    @get:PropertyName("lastModified") @set:PropertyName("lastModified") var lastModified: Long = System.currentTimeMillis(),
    @get:PropertyName("deleted") @set:PropertyName("deleted") var isDeleted: Boolean = false
)

data class FirestoreExpenseType(
    @get:PropertyName("id") @set:PropertyName("id") var id: Long = 0,
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("teamId") @set:PropertyName("teamId") var teamId: String = "",
    @get:PropertyName("lastModified") @set:PropertyName("lastModified") var lastModified: Long = System.currentTimeMillis(),
    @get:PropertyName("deleted") @set:PropertyName("deleted") var isDeleted: Boolean = false
)

data class FirestoreIncomeType(
    @get:PropertyName("id") @set:PropertyName("id") var id: Long = 0,
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("teamId") @set:PropertyName("teamId") var teamId: String = "",
    @get:PropertyName("lastModified") @set:PropertyName("lastModified") var lastModified: Long = System.currentTimeMillis(),
    @get:PropertyName("deleted") @set:PropertyName("deleted") var isDeleted: Boolean = false
)

data class FirestoreTeam(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("members") @set:PropertyName("members") var members: List<String> = emptyList(),
    @get:PropertyName("createdBy") @set:PropertyName("createdBy") var createdBy: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long = System.currentTimeMillis()
)

data class FirestoreUserProfile(
    @get:PropertyName("email") @set:PropertyName("email") var email: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String? = null,
    @get:PropertyName("playerPreference") @set:PropertyName("playerPreference") var playerPreference: String? = null,
    @get:PropertyName("mobileNumber") @set:PropertyName("mobileNumber") var mobileNumber: String? = null,
    @get:PropertyName("alternateMobileNumber") @set:PropertyName("alternateMobileNumber") var alternateMobileNumber: String? = null,
    @get:PropertyName("additionalResponsibility") @set:PropertyName("additionalResponsibility") var additionalResponsibility: String? = null,
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("teamId") @set:PropertyName("teamId") var teamId: String = "",
    @get:PropertyName("lastModified") @set:PropertyName("lastModified") var lastModified: Long = System.currentTimeMillis()
)

data class FirestoreUpcomingMatch(
    @get:PropertyName("dateUtcMillis") @set:PropertyName("dateUtcMillis") var dateUtcMillis: Long = 0,
    @get:PropertyName("team1") @set:PropertyName("team1") var team1: String = "",
    @get:PropertyName("team2") @set:PropertyName("team2") var team2: String = "",
    @get:PropertyName("groundName") @set:PropertyName("groundName") var groundName: String = "",
    @get:PropertyName("groundLocation") @set:PropertyName("groundLocation") var groundLocation: String = "",
    @get:PropertyName("groundFees") @set:PropertyName("groundFees") var groundFees: Double = 0.0,
    @get:PropertyName("ballProvided") @set:PropertyName("ballProvided") var ballProvided: Boolean = false,
    @get:PropertyName("noOfBalls") @set:PropertyName("noOfBalls") var noOfBalls: Int = 0,
    @get:PropertyName("ballName") @set:PropertyName("ballName") var ballName: String? = null,
    @get:PropertyName("overs") @set:PropertyName("overs") var overs: Int = 20,
    @get:PropertyName("teamId") @set:PropertyName("teamId") var teamId: String = "",
    @get:PropertyName("lastModified") @set:PropertyName("lastModified") var lastModified: Long = System.currentTimeMillis()
)

data class FirestoreMatchAvailability(
    @get:PropertyName("userEmail") @set:PropertyName("userEmail") var userEmail: String = "",
    @get:PropertyName("available") @set:PropertyName("available") var available: Boolean = false,
    @get:PropertyName("reason") @set:PropertyName("reason") var reason: String? = null,
    @get:PropertyName("teamId") @set:PropertyName("teamId") var teamId: String = "",
    @get:PropertyName("matchDate") @set:PropertyName("matchDate") var matchDate: Long = 0,
    @get:PropertyName("lastModified") @set:PropertyName("lastModified") var lastModified: Long = System.currentTimeMillis()
)
