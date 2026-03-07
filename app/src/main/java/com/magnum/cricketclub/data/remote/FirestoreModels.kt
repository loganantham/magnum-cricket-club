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
    @get:PropertyName("createdByEmail") @set:PropertyName("createdByEmail") var createdByEmail: String? = null,
    @get:PropertyName("lastModified") @set:PropertyName("lastModified") var lastModified: Long = System.currentTimeMillis(),
    @get:PropertyName("deleted") @set:PropertyName("deleted") var isDeleted: Boolean = false
) {
    // No-arg constructor for Firebase
    constructor() : this(0)
}

data class FirestoreExpenseType(
    @get:PropertyName("id") @set:PropertyName("id") var id: Long = 0,
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("teamId") @set:PropertyName("teamId") var teamId: String = "",
    @get:PropertyName("lastModified") @set:PropertyName("lastModified") var lastModified: Long = System.currentTimeMillis(),
    @get:PropertyName("deleted") @set:PropertyName("deleted") var isDeleted: Boolean = false
) {
    constructor() : this(0)
}

data class FirestoreIncomeType(
    @get:PropertyName("id") @set:PropertyName("id") var id: Long = 0,
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("teamId") @set:PropertyName("teamId") var teamId: String = "",
    @get:PropertyName("lastModified") @set:PropertyName("lastModified") var lastModified: Long = System.currentTimeMillis(),
    @get:PropertyName("deleted") @set:PropertyName("deleted") var isDeleted: Boolean = false
) {
    constructor() : this(0)
}

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
    @get:PropertyName("id") @set:PropertyName("id") var id: Long = 0,
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
    @get:PropertyName("matchType") @set:PropertyName("matchType") var matchType: String = "MAGNUM_MATCH",
    @get:PropertyName("team1FeesCollected") @set:PropertyName("team1FeesCollected") var team1FeesCollected: Boolean = false,
    @get:PropertyName("team2FeesCollected") @set:PropertyName("team2FeesCollected") var team2FeesCollected: Boolean = false,
    @get:PropertyName("groundFeesShared") @set:PropertyName("groundFeesShared") var groundFeesShared: Boolean = false,
    @get:PropertyName("team1FeesStatus") @set:PropertyName("team1FeesStatus") var team1FeesStatus: String = "PENDING",
    @get:PropertyName("team2FeesStatus") @set:PropertyName("team2FeesStatus") var team2FeesStatus: String = "PENDING",
    @get:PropertyName("team1PendingAmount") @set:PropertyName("team1PendingAmount") var team1PendingAmount: Double = 0.0,
    @get:PropertyName("team2PendingAmount") @set:PropertyName("team2PendingAmount") var team2PendingAmount: Double = 0.0,
    @get:PropertyName("teamId") @set:PropertyName("teamId") var teamId: String = "",
    @get:PropertyName("lastModified") @set:PropertyName("lastModified") var lastModified: Long = System.currentTimeMillis(),
    @get:PropertyName("deleted") @set:PropertyName("deleted") var isDeleted: Boolean = false
) {
    constructor() : this(0)
}

data class FirestoreMatchAvailability(
    @PropertyName("userEmail") val userEmail: String = "",
    @PropertyName("available") val available: Boolean = false,
    @PropertyName("reason") val reason: String? = null,
    @PropertyName("teamId") val teamId: String = "",
    @PropertyName("matchDate") val matchDate: Long = 0,
    @PropertyName("lastModified") val lastModified: Long = System.currentTimeMillis()
)

data class FirestoreAppConfig(
    @PropertyName("key") val key: String = "",
    @PropertyName("value") val value: String = "",
    @PropertyName("teamId") val teamId: String = "",
    @PropertyName("lastModified") val lastModified: Long = System.currentTimeMillis()
)

data class FirestoreContributionLedgerEntry(
    @PropertyName("id") val id: Long = 0,
    @PropertyName("contributorEmail") val contributorEmail: String = "",
    @PropertyName("year") val year: Int = 0,
    @PropertyName("monthIndex") val monthIndex: Int = 0,
    @PropertyName("status") val status: String = "",
    @PropertyName("pendingAmount") val pendingAmount: Double = 0.0,
    @PropertyName("teamId") val teamId: String = "",
    @PropertyName("lastModified") val lastModified: Long = System.currentTimeMillis()
)
