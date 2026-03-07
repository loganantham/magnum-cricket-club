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

    private fun Any?.asDouble(default: Double = 0.0): Double {
        return when (this) {
            is Number -> this.toDouble()
            is String -> this.toDoubleOrNull() ?: default
            else -> default
        }
    }

    private fun Any?.asBoolean(default: Boolean = false): Boolean {
        if (this == null) return default
        return when (this) {
            is Boolean -> this
            is Number -> this.toInt() != 0
            is String -> {
                val s = this.trim().lowercase()
                s == "true" || s == "1" || s == "yes" || s == "income" || s == "credit" || s == "inc"
            }
            else -> default
        }
    }

    /**
     * Finds the first non-null value among the provided keys.
     * Crucial for handling mixed schema versions in Firestore.
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

    private fun getSemanticMatchId(match: UpcomingMatch): Long {
        return Math.abs("${match.dateUtcMillis}_${match.team1}_${match.team2}".hashCode().toLong())
    }

    private fun extractTypeId(data: Map<String, Any?>, isIncome: Boolean): Long? {
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
        val userId = expense.userId ?: getCurrentUserId() ?: return
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

        val snapshot = try {
            firestore!!.collection("expenses")
                .whereEqualTo("teamId", teamId)
                .get()
                .await()
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        return snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data ?: return@mapNotNull null
                
                // Prioritize 'deleted' as per modern model, fallback to 'isDeleted'
                val isDeleted = data.getValue("deleted", "isDeleted").asBoolean()
                if (isDeleted) return@mapNotNull null
                
                var id = data.getValue("id").asLong()
                if (id == 0L) id = getStableId(doc.id)

                val amount = Math.abs(data.getValue("amount").asDouble())
                val description = data.getValue("description") as? String ?: ""
                val date = data.getValue("date").asLong(System.currentTimeMillis())
                
                // CRITICAL: Prioritize 'income' field name as seen in the database screenshot
                val isIncomeRaw = data.getValue("income", "isIncome")
                val isIncome = isIncomeRaw.asBoolean()

                val typeId = extractTypeId(data, isIncome)
                
                Expense(
                    id = id,
                    expenseTypeId = if (!isIncome) typeId else null,
                    incomeTypeId = if (isIncome) typeId else null,
                    amount = amount,
                    description = description,
                    date = date,
                    isIncome = isIncome,
                    createdByEmail = data.getValue("createdByEmail") as? String,
                    userId = data.getValue("userId") as? String
                )
            } catch (e: Exception) {
                null
            }
        }.distinctBy { it.id }
    }

    suspend fun deleteExpense(expenseId: Long) {
        if (!isFirebaseAvailable()) return
        firestore!!.collection("expenses").document(expenseId.toString())
            .update("deleted", true, "lastModified", System.currentTimeMillis()).await()
    }

    // Upcoming Match
    suspend fun uploadUpcomingMatch(match: UpcomingMatch) {
        if (!isFirebaseAvailable()) return
        val teamId = getCurrentTeamId()
        
        // Use a semantic ID for the document and the ID field to ensure cross-device consistency
        val semanticId = getSemanticMatchId(match)

        val firestoreMatch = FirestoreUpcomingMatch(
            id = semanticId,
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
            matchType = match.matchType,
            team1FeesCollected = match.team1FeesCollected,
            team2FeesCollected = match.team2FeesCollected,
            groundFeesShared = match.groundFeesShared,
            team1FeesStatus = match.team1FeesStatus,
            team2FeesStatus = match.team2FeesStatus,
            team1PendingAmount = match.team1PendingAmount,
            team2PendingAmount = match.team2PendingAmount,
            teamId = teamId,
            lastModified = System.currentTimeMillis()
        )

        firestore!!.collection("upcomingMatches")
            .document(semanticId.toString())
            .set(firestoreMatch)
            .await()
    }

    suspend fun downloadUpcomingMatches(): List<UpcomingMatch> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId()

        val snapshot = try {
            firestore!!.collection("upcomingMatches")
                .whereEqualTo("teamId", teamId)
                .get()
                .await()
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        return snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data ?: return@mapNotNull null
                val isDeleted = data.getValue("deleted", "isDeleted").asBoolean()
                if (isDeleted) return@mapNotNull null
                
                val dateUtcMillis = data.getValue("dateUtcMillis").asLong()
                val team1 = data.getValue("team1") as? String ?: ""
                val team2 = data.getValue("team2") as? String ?: ""
                
                // Always recalculate semantic ID for consistency
                val semanticId = Math.abs("${dateUtcMillis}_${team1}_${team2}".hashCode().toLong())

                UpcomingMatch(
                    id = semanticId,
                    dateUtcMillis = dateUtcMillis,
                    team1 = team1,
                    team2 = team2,
                    groundName = data.getValue("groundName") as? String ?: "",
                    groundLocation = data.getValue("groundLocation") as? String ?: "",
                    groundFees = data.getValue("groundFees").asDouble(),
                    ballProvided = data.getValue("ballProvided").asBoolean(),
                    noOfBalls = data.getValue("noOfBalls").asLong().toInt(),
                    ballName = data.getValue("ballName") as? String,
                    overs = data.getValue("overs").asLong().toInt().let { if (it == 0) 20 else it },
                    matchType = data.getValue("matchType") as? String ?: "MAGNUM_MATCH",
                    team1FeesCollected = data.getValue("team1FeesCollected").asBoolean(),
                    team2FeesCollected = data.getValue("team2FeesCollected").asBoolean(),
                    groundFeesShared = data.getValue("groundFeesShared").asBoolean(),
                    team1FeesStatus = data.getValue("team1FeesStatus") as? String ?: "PENDING",
                    team2FeesStatus = data.getValue("team2FeesStatus") as? String ?: "PENDING",
                    team1PendingAmount = data.getValue("team1PendingAmount").asDouble(),
                    team2PendingAmount = data.getValue("team2PendingAmount").asDouble()
                )
            } catch (e: Exception) {
                null
            }
        }.distinctBy { it.id } // This now deduplicates by semantic ID
    }

    suspend fun deleteUpcomingMatch(matchId: Long) {
        if (!isFirebaseAvailable()) return
        // Try deleting by the provided ID
        try {
            firestore!!.collection("upcomingMatches")
                .document(matchId.toString())
                .update("deleted", true, "lastModified", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete by ID, might be a random document ID: $matchId")
        }
    }

    /**
     * More robust deletion that finds all documents matching the match content
     */
    suspend fun deleteUpcomingMatchRobustly(match: UpcomingMatch) {
        if (!isFirebaseAvailable()) return
        val teamId = getCurrentTeamId()
        
        try {
            val snapshot = firestore!!.collection("upcomingMatches")
                .whereEqualTo("teamId", teamId)
                .whereEqualTo("dateUtcMillis", match.dateUtcMillis)
                .whereEqualTo("team1", match.team1)
                .whereEqualTo("team2", match.team2)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.update(doc.reference, "deleted", true, "lastModified", System.currentTimeMillis())
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in robust deletion", e)
        }
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
            firestore!!.collection("userProfiles")
                .whereEqualTo("teamId", teamId)
                .get()
                .await()
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        return snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data ?: return@mapNotNull null

                UserProfile(
                    email = data.getValue("email") as? String ?: doc.id,
                    name = data.getValue("name") as? String,
                    playerPreference = data.getValue("playerPreference") as? String,
                    mobileNumber = data.getValue("mobileNumber") as? String,
                    alternateMobileNumber = data.getValue("alternateMobileNumber") as? String,
                    additionalResponsibility = data.getValue("additionalResponsibility") as? String,
                    userId = data.getValue("userId") as? String ?: ""
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun deleteUserProfile(email: String) {
        if (!isFirebaseAvailable()) return
        firestore!!.collection("userProfiles").document(email).delete().await()
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
        val firestoreConfig = FirestoreAppConfig(key = config.key, value = config.value, teamId = teamId, lastModified = System.currentTimeMillis())
        firestore!!.collection("appConfigs").document("${teamId}_${config.key}").set(firestoreConfig).await()
    }

    suspend fun downloadAllAppConfigs(): List<AppConfig> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId()
        val snapshot = try { firestore!!.collection("appConfigs").whereEqualTo("teamId", teamId).get().await() } catch (e: Exception) { null } ?: return emptyList()
        return snapshot.documents.mapNotNull { doc ->
            val data = doc.toObject(FirestoreAppConfig::class.java) ?: return@mapNotNull null
            AppConfig(key = data.key, value = data.value)
        }
    }

    // Contribution Ledger
    suspend fun uploadContributionLedgerEntry(entry: ContributionLedgerEntry) {
        if (!isFirebaseAvailable()) return
        val teamId = getCurrentTeamId()
        val firestoreEntry = FirestoreContributionLedgerEntry(id = entry.id, contributorEmail = entry.contributorEmail, year = entry.year, monthIndex = entry.monthIndex, status = entry.status, pendingAmount = entry.pendingAmount, teamId = teamId, lastModified = System.currentTimeMillis())
        firestore!!.collection("contributionLedger").document("${teamId}_${entry.contributorEmail}_${entry.year}_${entry.monthIndex}").set(firestoreEntry).await()
    }

    suspend fun downloadAllContributionLedgerEntries(): List<ContributionLedgerEntry> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId()
        val snapshot = try { firestore!!.collection("contributionLedger").whereEqualTo("teamId", teamId).get().await() } catch (e: Exception) { null } ?: return emptyList()
        return snapshot.documents.mapNotNull { doc ->
            val data = doc.toObject(FirestoreContributionLedgerEntry::class.java) ?: return@mapNotNull null
            ContributionLedgerEntry(id = data.id, contributorEmail = data.contributorEmail, year = data.year, monthIndex = data.monthIndex, status = data.status, pendingAmount = data.pendingAmount)
        }
    }

    // Authentication
    fun isUserSignedIn(): Boolean = auth?.currentUser != null
    fun getCurrentUserEmail(): String? = auth?.currentUser?.email
    fun getCurrentUserUid(): String? = auth?.currentUser?.uid

    suspend fun downloadExpenseTypes(): List<ExpenseType> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId()
        val snapshot = try {
            firestore!!.collection("expenseTypes")
                .whereEqualTo("teamId", teamId)
                .get()
                .await()
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        return snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data ?: return@mapNotNull null
                if (data.getValue("deleted", "isDeleted").asBoolean()) return@mapNotNull null
                
                var id = data.getValue("id").asLong()
                if (id == 0L) id = getStableId(doc.id)
                
                ExpenseType(id = id, name = data.getValue("name") as? String ?: "", description = data.getValue("description") as? String ?: "")
            } catch (e: Exception) { null }
        }.distinctBy { it.id }
    }

    suspend fun uploadExpenseType(expenseType: ExpenseType) {
        if (!isFirebaseAvailable()) return
        val teamId = getCurrentTeamId()
        val firestoreType = FirestoreExpenseType(id = expenseType.id, name = expenseType.name, description = expenseType.description, teamId = teamId, lastModified = System.currentTimeMillis())
        firestore!!.collection("expenseTypes").document(expenseType.id.toString()).set(firestoreType).await()
    }

    suspend fun deleteExpenseType(typeId: Long) {
        if (!isFirebaseAvailable()) return
        firestore!!.collection("expenseTypes").document(typeId.toString()).update("deleted", true, "lastModified", System.currentTimeMillis()).await()
    }

    suspend fun downloadIncomeTypes(): List<IncomeType> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId()
        val snapshot = try {
            firestore!!.collection("incomeTypes")
                .whereEqualTo("teamId", teamId)
                .get()
                .await()
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        return snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data ?: return@mapNotNull null
                if (data.getValue("deleted", "isDeleted").asBoolean()) return@mapNotNull null
                
                var id = data.getValue("id").asLong()
                if (id == 0L) id = getStableId(doc.id)
                
                IncomeType(id = id, name = data.getValue("name") as? String ?: "", description = data.getValue("description") as? String ?: "")
            } catch (e: Exception) { null }
        }.distinctBy { it.id }
    }

    suspend fun uploadIncomeType(incomeType: IncomeType) {
        if (!isFirebaseAvailable()) return
        val teamId = getCurrentTeamId()
        val firestoreType = FirestoreIncomeType(id = incomeType.id, name = incomeType.name, description = incomeType.description, teamId = teamId, lastModified = System.currentTimeMillis())
        firestore!!.collection("incomeTypes").document(incomeType.id.toString()).set(firestoreType).await()
    }

    suspend fun deleteIncomeType(typeId: Long) {
        if (!isFirebaseAvailable()) return
        firestore!!.collection("incomeTypes").document(typeId.toString()).update("deleted", true, "lastModified", System.currentTimeMillis()).await()
    }
}
