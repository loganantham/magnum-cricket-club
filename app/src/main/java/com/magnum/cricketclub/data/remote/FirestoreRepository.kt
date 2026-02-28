package com.magnum.cricketclub.data.remote

import com.magnum.cricketclub.data.Expense
import com.magnum.cricketclub.data.ExpenseType
import com.magnum.cricketclub.data.IncomeType
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
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

    private fun getCurrentUserId(): String? = auth?.currentUser?.uid
    private fun getCurrentTeamId(): String {
        // Use "magnum" as the team ID for all users
        return "magnum"
    }
    
    private fun isFirebaseAvailable(): Boolean {
        return firestore != null && auth != null
    }

    // Expenses
    suspend fun uploadExpense(expense: Expense) {
        if (!isFirebaseAvailable()) return
        val userId = getCurrentUserId() ?: return
        val teamId = getCurrentTeamId() ?: return

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
            lastModified = System.currentTimeMillis()
        )

        firestore!!.collection("expenses")
            .document(expense.id.toString())
            .set(firestoreExpense)
            .await()
    }

    suspend fun downloadExpenses(): List<Expense> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId() ?: return emptyList()

        val snapshot = firestore!!.collection("expenses")
            .whereEqualTo("teamId", teamId)
            .whereEqualTo("isDeleted", false)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val data = doc.toObject(FirestoreExpense::class.java) ?: return@mapNotNull null
            Expense(
                id = data.id,
                expenseTypeId = data.expenseTypeId,
                incomeTypeId = data.incomeTypeId,
                amount = data.amount,
                description = data.description,
                date = data.date,
                isIncome = data.isIncome
            )
        }
    }

    suspend fun deleteExpense(expenseId: Long) {
        if (!isFirebaseAvailable()) return
        val expenseRef = firestore!!.collection("expenses").document(expenseId.toString())
        expenseRef.update("isDeleted", true, "lastModified", System.currentTimeMillis()).await()
    }

    // Note: Real-time observation can be implemented using callbackFlow if needed
    // For now, we use periodic sync instead

    // Expense Types
    suspend fun uploadExpenseType(expenseType: ExpenseType) {
        if (!isFirebaseAvailable()) return
        val teamId = getCurrentTeamId() ?: return

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
        val teamId = getCurrentTeamId() ?: return emptyList()

        val snapshot = firestore!!.collection("expenseTypes")
            .whereEqualTo("teamId", teamId)
            .whereEqualTo("isDeleted", false)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val data = doc.toObject(FirestoreExpenseType::class.java) ?: return@mapNotNull null
            ExpenseType(
                id = data.id,
                name = data.name,
                description = data.description
            )
        }
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
        val teamId = getCurrentTeamId() ?: return

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
        val teamId = getCurrentTeamId() ?: return emptyList()

        val snapshot = firestore!!.collection("incomeTypes")
            .whereEqualTo("teamId", teamId)
            .whereEqualTo("isDeleted", false)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val data = doc.toObject(FirestoreIncomeType::class.java) ?: return@mapNotNull null
            IncomeType(
                id = data.id,
                name = data.name,
                description = data.description
            )
        }
    }

    suspend fun deleteIncomeType(typeId: Long) {
        if (!isFirebaseAvailable()) return
        firestore!!.collection("incomeTypes")
            .document(typeId.toString())
            .update("isDeleted", true, "lastModified", System.currentTimeMillis())
            .await()
    }

    // User Profiles
    suspend fun uploadUserProfile(userProfile: com.magnum.cricketclub.data.UserProfile) {
        if (!isFirebaseAvailable()) return
        val userId = getCurrentUserId() ?: return
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

    suspend fun downloadAllUserProfiles(): List<com.magnum.cricketclub.data.UserProfile> {
        if (!isFirebaseAvailable()) return emptyList()
        val teamId = getCurrentTeamId()

        // Get all profiles (we'll filter and update teamId if needed)
        val allProfilesSnapshot = firestore!!.collection("userProfiles")
            .get()
            .await()

        // Update profiles with blank/null/missing teamId to "magnum"
        allProfilesSnapshot.documents.forEach { doc ->
            val data = doc.toObject(FirestoreUserProfile::class.java)
            val currentTeamId = data?.teamId ?: ""
            
            // Update if teamId is blank, null, or missing
            if (currentTeamId.isEmpty() || currentTeamId.isBlank()) {
                try {
                    doc.reference.update("teamId", teamId, "lastModified", System.currentTimeMillis()).await()
                    android.util.Log.d("FirestoreRepository", "Updated teamId to 'magnum' for user: ${doc.id}")
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreRepository", "Error updating teamId for user: ${doc.id}", e)
                }
            }
        }

        // Now get all profiles with the teamId (including newly updated ones)
        val snapshot = firestore!!.collection("userProfiles")
            .whereEqualTo("teamId", teamId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val data = doc.toObject(FirestoreUserProfile::class.java) ?: return@mapNotNull null
            com.magnum.cricketclub.data.UserProfile(
                email = data.email,
                name = data.name,
                playerPreference = data.playerPreference,
                mobileNumber = data.mobileNumber,
                alternateMobileNumber = data.alternateMobileNumber,
                additionalResponsibility = data.additionalResponsibility
            )
        }
    }

    // Authentication
    fun isUserSignedIn(): Boolean = auth?.currentUser != null
    fun getCurrentUserEmail(): String? = auth?.currentUser?.email
    fun getCurrentUserUid(): String? = auth?.currentUser?.uid
}
