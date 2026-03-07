package com.magnum.cricketclub.data.remote

import android.util.Log
import com.magnum.cricketclub.data.AppConfig
import com.magnum.cricketclub.data.ContributionLedgerEntry
import com.magnum.cricketclub.data.Expense
import com.magnum.cricketclub.data.ExpenseType
import com.magnum.cricketclub.data.IncomeType
import com.magnum.cricketclub.data.UpcomingMatch
import com.magnum.cricketclub.data.UserProfile
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val TAG = "FirestoreRepository"
    private val firestore = try {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        null
    }
    
    private val auth = try {
        com.google.firebase.auth.FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }

    fun getCurrentUserId(): String? = auth?.currentUser?.uid
    fun getCurrentTeamId(): String {
        return "magnum"
    }
    
    fun isFirebaseAvailable(): Boolean {
        return firestore != null && auth != null
    }

    // Robust type conversion helpers
    private fun Any?.asLong(default: Long = 0L): Long {
        return when (this) {
            is Number -> this.toLong()
            is String -> this.toLongOrNull() ?: default
            else -> default
        }
    }

    private fun Any?.asLongOrNull(): Long? {
        val l = when (this) {
            is Number -> this.toLong()
            is String -> this.toLongOrNull()
            else -> null
        }
        return if (l == null) null else l // Allow 0L if explicitly present, though usually we want non-zero
    }

    private fun Any?.asDouble(default: Double = 0.0): Double {
        return when (this) {
            is Number -> this.toDouble()
            is String -> this.toDoubleOrNull() ?: default
            else -> default
        }
    }

    private fun Any?.asBoolean(default: Boolean = false): Boolean {
        return when (this) {
            is Boolean -> this
            is Number -> this.toInt() != 0
            is String -> {
                val s = this.lowercase()
                s == "true" || s == "1" || s == "yes" || s == "income" || s == "credit"
            }
            else -> default
        }
    }

    /**
     * Finds the first non-null value for the given keys.
     * Crucially, it skips keys that are present in the map but have a null value.
     */
    private fun Map<String, Any?>?.getValue(vararg keys: String): Any? {
        if (this == null) return null
        for (key in keys) {
            val value = this[key]
            if (value != null) return value
        }
        return null
    }

    private fun getStableId(docId: String): Long {
        val numericId = docId.toLongOrNull()
        if (numericId != null && numericId != 0L) return numericId
        return Math.abs(docId.hashCode().toLong())
    }

    private fun extractTypeId(data: Map<String, Any?>, isIncome: Boolean): Long? {
        // Prioritize the correct field based on whether it's an income or expense
        val raw = if (isIncome) {
            data.getValue("incomeTypeId", "income_type_id", "expenseTypeId", "expense_type_id", "categoryId", "category_id", "typeId", "type_id", "category")
        } else {
            data.getValue("expenseTypeId", "expense_type_id", "incomeTypeId", "income_type_id", "categoryId", "category_id", "typeId", "type_id", "category")
        }
        
        return when (raw) {
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull() ?: getStableId(raw)
            else -> null
        }
    }

    // Expenses
    suspend fun uploadExpense(expense: Expense) {
        if (!isFirebaseAvailable()) return
        val userId = getCurrentUserId() ?: return
        val teamId = getCurrentTeamId()

        val firestoreExpense = FirestoreExpense(
            id = expense.id,
            expenseTypeId = expense.expenseTypeId,
            incomeTypeId = expense.incomeTypeId,
            amount = expense.amount,
            description = expense.description,
            date = expense.date,
            isIncome = expense.isIncome,
            userId = userId,
            teamId = teamId,
            createdByEmail = expense.createdByEmail,
            lastModified = System.currentTimeMillis()
        )

        firestore!!.collection("expenses")
            .document(expense.id.toString())
            .set(firestoreExpense)
            .await()
    }

    suspend fun downloadExpenses(): List<Expense> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId()

        val expenseCollections = listOf("expenses", "incomes", "transactions")
        val snapshots = mutableListOf<com.google.firebase.firestore.QuerySnapshot>()
        
        for (coll in expenseCollections) {
            try { snapshots.add(firestore!!.collection(coll).get().await()) } catch (e: Exception) {}
        }

        return snapshots.flatMap { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    
                    val isDeleted = data.getValue("isDeleted", "deleted", "is_deleted").asBoolean()
                    if (isDeleted) return@mapNotNull null
                    
                    val docTeamId = data.getValue("teamId", "team_id", "team") as? String ?: ""
                    if (docTeamId.isNotEmpty() && docTeamId != teamId) return@mapNotNull null

                    // Ensure we have a valid numeric ID. Fallback to doc.id if field is missing or 0.
                    var id = data.getValue("id", "expenseId", "expense_id").asLong()
                    if (id == 0L) id = getStableId(doc.id)

                    val amountRaw = data.getValue("amount", "value", "total").asDouble()
                    val amount = Math.abs(amountRaw)
                    val description = data.getValue("description", "desc", "note", "remarks") as? String ?: ""
                    val date = data.getValue("date", "timestamp", "time", "createdAt").asLong(System.currentTimeMillis())
                    
                    // Prioritize the 'income' field as explicitly requested by user
                    var isIncome = data["income"].asBoolean() || 
                                  data.getValue("isIncome", "is_income", "isIncomeType", "is_credit", "credit").asBoolean()
                    
                    val typeVal = data.getValue("type", "entryType", "category", "transactionType", "kind")
                    if (typeVal is String) {
                        val s = typeVal.lowercase()
                        if (s == "income" || s == "credit" || s == "contribution" || s == "opening balance") isIncome = true
                        else if (s == "expense" || s == "debit") isIncome = false
                    }
                    
                    if (doc.reference.parent.id == "incomes") isIncome = true
                    if (description.contains("Opening Balance", ignoreCase = true) || 
                        description.contains("Contribution", ignoreCase = true) ||
                        description.contains("Refund", ignoreCase = true)) {
                        isIncome = true
                    }
                    
                    // Final check: if amount was negative and no explicit income flag found
                    if (amountRaw < 0 && data["income"] == null && data["isIncome"] == null) {
                        isIncome = false
                    }

                    val typeId = extractTypeId(data, isIncome)
                    val expenseTypeId = if (!isIncome) typeId else null
                    val incomeTypeId = if (isIncome) typeId else null
                    
                    val createdByEmail = data.getValue("createdByEmail", "added_by", "user_email", "email", "addedBy", "user") as? String

                    Expense(
                        id = id,
                        expenseTypeId = expenseTypeId,
                        incomeTypeId = incomeTypeId,
                        amount = amount,
                        description = description,
                        date = date,
                        isIncome = isIncome,
                        createdByEmail = createdByEmail
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping expense doc: ${doc.id}", e)
                    null
                }
            }
        }.distinctBy { it.id }
    }

    suspend fun deleteExpense(expenseId: Long) {
        if (!isFirebaseAvailable()) return
        val expenseRef = firestore!!.collection("expenses").document(expenseId.toString())
        expenseRef.update("isDeleted", true, "lastModified", System.currentTimeMillis()).await()
    }

    // Expense Types
    suspend fun uploadExpenseType(expenseType: ExpenseType) {
        if (!isFirebaseAvailable()) return
        val teamId = getCurrentTeamId()

        val firestoreType = FirestoreExpenseType(
            id = expenseType.id,
            name = expenseType.name,
            description = expenseType.description,
            teamId = teamId,
            lastModified = System.currentTimeMillis()
        )

        firestore!!.collection("expenseTypes")
            .document(expenseType.id.toString())
            .set(firestoreType)
            .await()
    }

    suspend fun downloadExpenseTypes(): List<ExpenseType> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId()

        val collections = listOf("expenseTypes", "expense_types", "categories", "expenseCategories", "types")
        val snapshots = mutableListOf<com.google.firebase.firestore.QuerySnapshot>()
        
        for (coll in collections) {
            try { snapshots.add(firestore!!.collection(coll).get().await()) } catch (e: Exception) {}
        }

        return snapshots.flatMap { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val isDeleted = data.getValue("isDeleted", "deleted", "is_deleted").asBoolean()
                    if (isDeleted) return@mapNotNull null
                    
                    val docTeamId = data.getValue("teamId", "team_id") as? String ?: ""
                    if (docTeamId.isNotEmpty() && docTeamId != teamId) return@mapNotNull null

                    var id = data.getValue("id", "typeId", "type_id", "categoryId", "category_id").asLong()
                    if (id == 0L) id = getStableId(doc.id)
                    
                    val name = data.getValue("name", "title", "label", "typeName") as? String ?: ""
                    val description = data.getValue("description", "desc", "info") as? String ?: ""

                    ExpenseType(id = id, name = name, description = description)
                } catch (e: Exception) {
                    null
                }
            }
        }.distinctBy { it.id }
    }

    suspend fun deleteExpenseType(typeId: Long) {
        if (!isFirebaseAvailable()) return
        firestore!!.collection("expenseTypes")
            .document(typeId.toString())
            .update("isDeleted", true, "lastModified", System.currentTimeMillis())
            .await()
    }

    // Income Types
    suspend fun uploadIncomeType(incomeType: IncomeType) {
        if (!isFirebaseAvailable()) return
        val teamId = getCurrentTeamId()

        val firestoreType = FirestoreIncomeType(
            id = incomeType.id,
            name = incomeType.name,
            description = incomeType.description,
            teamId = teamId,
            lastModified = System.currentTimeMillis()
        )

        firestore!!.collection("incomeTypes")
            .document(incomeType.id.toString())
            .set(firestoreType)
            .await()
    }

    suspend fun downloadIncomeTypes(): List<IncomeType> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId()

        val collections = listOf("incomeTypes", "income_types", "incomeCategories", "income_categories")
        val snapshots = mutableListOf<com.google.firebase.firestore.QuerySnapshot>()
        
        for (coll in collections) {
            try { snapshots.add(firestore!!.collection(coll).get().await()) } catch (e: Exception) {}
        }

        return snapshots.flatMap { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val isDeleted = data.getValue("isDeleted", "deleted", "is_deleted").asBoolean()
                    if (isDeleted) return@mapNotNull null
                    
                    val docTeamId = data.getValue("teamId", "team_id") as? String ?: ""
                    if (docTeamId.isNotEmpty() && docTeamId != teamId) return@mapNotNull null

                    var id = data.getValue("id", "typeId", "type_id", "categoryId", "category_id").asLong()
                    if (id == 0L) id = getStableId(doc.id)
                    
                    val name = data.getValue("name", "title", "label", "typeName") as? String ?: ""
                    val description = data.getValue("description", "desc", "info") as? String ?: ""

                    IncomeType(id = id, name = name, description = description)
                } catch (e: Exception) {
                    null
                }
            }
        }.distinctBy { it.id }
    }

    suspend fun deleteIncomeType(typeId: Long) {
        if (!isFirebaseAvailable()) return
        firestore!!.collection("incomeTypes")
            .document(typeId.toString())
            .update("isDeleted", true, "lastModified", System.currentTimeMillis())
            .await()
    }

    // User Profiles
    suspend fun uploadUserProfile(userProfile: UserProfile) {
        if (!isFirebaseAvailable()) return
        val userId = getCurrentUserId() ?: ""
        val teamId = getCurrentTeamId()

        val firestoreProfile = FirestoreUserProfile(
            email = userProfile.email,
            name = userProfile.name,
            playerPreference = userProfile.playerPreference,
            mobileNumber = userProfile.mobileNumber,
            alternateMobileNumber = userProfile.alternateMobileNumber,
            additionalResponsibility = userProfile.additionalResponsibility,
            userId = userId,
            teamId = teamId,
            lastModified = System.currentTimeMillis()
        )

        firestore!!.collection("userProfiles")
            .document(userProfile.email)
            .set(firestoreProfile)
            .await()
    }

    suspend fun downloadAllUserProfiles(): List<UserProfile> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId()

        val snapshot = try {
            firestore!!.collection("userProfiles").get().await()
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        return snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data ?: return@mapNotNull null
                val docTeamId = data.getValue("teamId", "team_id") as? String ?: ""
                
                if (docTeamId.isNotEmpty() && docTeamId != teamId) return@mapNotNull null

                UserProfile(
                    email = data.getValue("email", "userEmail") as? String ?: doc.id,
                    name = data.getValue("name", "displayName", "fullName") as? String,
                    playerPreference = data.getValue("playerPreference", "preference", "role") as? String,
                    mobileNumber = data.getValue("mobileNumber", "mobile", "phone", "contact") as? String,
                    alternateMobileNumber = data.getValue("alternateMobileNumber", "alternateMobile", "secondaryPhone") as? String,
                    additionalResponsibility = data.getValue("additionalResponsibility", "responsibility", "roles", "adminRole") as? String
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun deleteUserProfile(email: String) {
        if (!isFirebaseAvailable()) return
        firestore!!.collection("userProfiles")
            .document(email)
            .delete()
            .await()
    }

    // Upcoming Match
    suspend fun uploadUpcomingMatch(match: UpcomingMatch) {
        if (!isFirebaseAvailable()) return
        val teamId = getCurrentTeamId()

        val firestoreMatch = FirestoreUpcomingMatch(
            dateUtcMillis = match.dateUtcMillis,
            team1 = match.team1,
            team2 = match.team2,
            groundName = match.groundName,
            groundLocation = match.groundLocation,
            groundFees = match.groundFees,
            ballProvided = match.ballProvided,
            noOfBalls = match.noOfBalls,
            ballName = match.ballName,
            overs = match.overs,
            teamId = teamId,
            lastModified = System.currentTimeMillis()
        )

        firestore!!.collection("upcomingMatches")
            .document(teamId)
            .set(firestoreMatch)
            .await()
    }

    suspend fun downloadUpcomingMatch(): UpcomingMatch? {
        if (!isFirebaseAvailable()) return null
        val teamId = getCurrentTeamId()

        val doc = try {
            firestore!!.collection("upcomingMatches")
                .document(teamId)
                .get()
                .await()
        } catch (e: Exception) {
            null
        } ?: return null

        if (!doc.exists()) return null

        val data = doc.toObject(FirestoreUpcomingMatch::class.java) ?: return null
        return UpcomingMatch(
            dateUtcMillis = data.dateUtcMillis,
            team1 = data.team1,
            team2 = data.team2,
            groundName = data.groundName,
            groundLocation = data.groundLocation,
            groundFees = data.groundFees,
            ballProvided = data.ballProvided,
            noOfBalls = data.noOfBalls,
            ballName = data.ballName,
            overs = data.overs
        )
    }

    // Match Availability
    suspend fun uploadMatchAvailability(available: Boolean, matchDate: Long, reason: String? = null, email: String? = null) {
        if (!isFirebaseAvailable()) return
        val userEmail = email ?: getCurrentUserEmail() ?: return
        val teamId = getCurrentTeamId()

        val firestoreAvailability = FirestoreMatchAvailability(
            userEmail = userEmail,
            available = available,
            reason = reason,
            teamId = teamId,
            matchDate = matchDate,
            lastModified = System.currentTimeMillis()
        )

        firestore!!.collection("matchAvailability")
            .document("${userEmail}_${matchDate}")
            .set(firestoreAvailability)
            .await()
    }

    suspend fun downloadMatchAvailability(matchDate: Long): FirestoreMatchAvailability? {
        if (!isFirebaseAvailable()) return null
        val userEmail = getCurrentUserEmail() ?: return null

        val doc = try {
            firestore!!.collection("matchAvailability")
                .document("${userEmail}_${matchDate}")
                .get()
                .await()
        } catch (e: Exception) {
            null
        } ?: return null

        if (!doc.exists()) return null

        return doc.toObject(FirestoreMatchAvailability::class.java)
    }

    suspend fun downloadAllMatchAvailabilities(matchDate: Long, available: Boolean? = null): List<FirestoreMatchAvailability> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId()

        var query = firestore!!.collection("matchAvailability")
            .whereEqualTo("teamId", teamId)
            .whereEqualTo("matchDate", matchDate)

        if (available != null) {
            query = query.whereEqualTo("available", available)
        }

        val snapshot = try { query.get().await() } catch (e: Exception) { null } ?: return emptyList()

        return snapshot.documents.mapNotNull { it.toObject(FirestoreMatchAvailability::class.java) }
    }

    // App Config
    suspend fun uploadAppConfig(config: AppConfig) {
        if (!isFirebaseAvailable()) return
        val teamId = getCurrentTeamId()

        val firestoreConfig = FirestoreAppConfig(
            key = config.key,
            value = config.value,
            teamId = teamId,
            lastModified = System.currentTimeMillis()
        )

        firestore!!.collection("appConfigs")
            .document("${teamId}_${config.key}")
            .set(firestoreConfig)
            .await()
    }

    suspend fun downloadAllAppConfigs(): List<AppConfig> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId()

        val snapshot = try {
            firestore!!.collection("appConfigs")
                .whereEqualTo("teamId", teamId)
                .get()
                .await()
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        return snapshot.documents.mapNotNull { doc ->
            val data = doc.toObject(FirestoreAppConfig::class.java) ?: return@mapNotNull null
            AppConfig(key = data.key, value = data.value)
        }
    }

    // Contribution Ledger
    suspend fun uploadContributionLedgerEntry(entry: ContributionLedgerEntry) {
        if (!isFirebaseAvailable()) return
        val teamId = getCurrentTeamId()

        val firestoreEntry = FirestoreContributionLedgerEntry(
            id = entry.id,
            contributorEmail = entry.contributorEmail,
            year = entry.year,
            monthIndex = entry.monthIndex,
            status = entry.status,
            pendingAmount = entry.pendingAmount,
            teamId = teamId,
            lastModified = System.currentTimeMillis()
        )

        firestore!!.collection("contributionLedger")
            .document("${teamId}_${entry.contributorEmail}_${entry.year}_${entry.monthIndex}")
            .set(firestoreEntry)
            .await()
    }

    suspend fun downloadAllContributionLedgerEntries(): List<ContributionLedgerEntry> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId()

        val snapshot = try {
            firestore!!.collection("contributionLedger")
                .whereEqualTo("teamId", teamId)
                .get()
                .await()
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        return snapshot.documents.mapNotNull { doc ->
            val data = doc.toObject(FirestoreContributionLedgerEntry::class.java) ?: return@mapNotNull null
            ContributionLedgerEntry(
                id = data.id,
                contributorEmail = data.contributorEmail,
                year = data.year,
                monthIndex = data.monthIndex,
                status = data.status,
                pendingAmount = data.pendingAmount
            )
        }
    }

    // Authentication
    fun isUserSignedIn(): Boolean = auth?.currentUser != null
    fun getCurrentUserEmail(): String? = auth?.currentUser?.email
    fun getCurrentUserUid(): String? = auth?.currentUser?.uid
}
