package com.magnum.cricketclub.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contribution_ledger",
    indices = [Index(value = ["contributorEmail", "year"], name = "index_contribution_ledger_email_year")]
)
data class ContributionLedgerEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contributorEmail: String,
    val year: Int,
    val monthIndex: Int, // 0-based (0 = Jan)
    val status: String,
    val pendingAmount: Double
)
