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
    @PropertyName("userId") val userId: String = "",
    @PropertyName("teamId") val teamId: String = "",
    @PropertyName("lastModified") val lastModified: Long = System.currentTimeMillis()
)
