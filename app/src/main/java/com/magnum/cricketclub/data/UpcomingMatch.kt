package com.magnum.cricketclub.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upcoming_matches")
data class UpcomingMatch(
    @PrimaryKey val id: Long = 0, // Manual ID based on content to prevent duplicates
    val dateUtcMillis: Long,
    val team1: String,
    val team2: String,
    val groundName: String,
    val groundLocation: String,
    val groundFees: Double = 0.0,
    val ballProvided: Boolean = false,
    val noOfBalls: Int = 0,
    val ballName: String? = null, // SF Yorker, SF True Test
    val overs: Int = 20,
    val matchType: String = "MAGNUM_MATCH", // MAGNUM_MATCH, MAGNUM_GROUND_MATCH
    val team1FeesCollected: Boolean = false,
    val team2FeesCollected: Boolean = false,
    val groundFeesShared: Boolean = false,
    val team1FeesStatus: String = "PENDING", // PENDING, PARTIAL, DONE
    val team2FeesStatus: String = "PENDING", // PENDING, PARTIAL, DONE
    val team1PendingAmount: Double = 0.0,
    val team2PendingAmount: Double = 0.0,
    val lastModified: Long = 0L,
    val isDeleted: Boolean = false
) {
    companion object {
        fun generateSemanticId(dateUtcMillis: Long, team1: String, team2: String): Long {
            return Math.abs("${dateUtcMillis}_${team1.trim().lowercase()}_${team2.trim().lowercase()}".hashCode().toLong())
        }
    }
}
