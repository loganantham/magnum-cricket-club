package com.magnum.cricketclub.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ContributionLedgerDao {
    @Query(
        "SELECT * FROM contribution_ledger " +
                "WHERE contributorEmail = :email AND year = :year"
    )
    suspend fun getEntriesForContributorAndYear(
        email: String,
        year: Int
    ): List<ContributionLedgerEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entry: ContributionLedgerEntry): Long

    @Update
    suspend fun update(entry: ContributionLedgerEntry)
}

