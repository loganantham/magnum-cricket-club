package com.magnum.cricketclub.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UpcomingMatchDao {
    @Query("SELECT * FROM upcoming_matches ORDER BY dateUtcMillis ASC")
    fun getAllMatches(): Flow<List<UpcomingMatch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: UpcomingMatch): Long

    @Update
    suspend fun updateMatch(match: UpcomingMatch)

    @Delete
    suspend fun deleteMatch(match: UpcomingMatch)

    @Query("DELETE FROM upcoming_matches WHERE id = :matchId")
    suspend fun deleteMatchById(matchId: Long)
}
