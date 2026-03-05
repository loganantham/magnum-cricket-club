package com.magnum.cricketclub.utils

import android.content.Context
import com.magnum.cricketclub.data.UpcomingMatch

object UpcomingMatchStore {
    private const val PREFS_NAME = "upcoming_match_prefs"

    private const val KEY_DATE_UTC_MILLIS = "date_utc_millis"
    private const val KEY_TEAM1 = "team1"
    private const val KEY_TEAM2 = "team2"
    private const val KEY_GROUND_NAME = "ground_name"
    private const val KEY_GROUND_LOCATION = "ground_location"
    private const val KEY_GROUND_FEES = "ground_fees"
    private const val KEY_BALL_PROVIDED = "ball_provided"
    private const val KEY_NO_OF_BALLS = "no_of_balls"
    private const val KEY_BALL_NAME = "ball_name"
    private const val KEY_OVERS = "overs"

    fun load(context: Context): UpcomingMatch? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val date = prefs.getLong(KEY_DATE_UTC_MILLIS, 0L)
        if (date <= 0L) return null

        val team1 = prefs.getString(KEY_TEAM1, null)?.trim().orEmpty()
        val team2 = prefs.getString(KEY_TEAM2, null)?.trim().orEmpty()
        val groundName = prefs.getString(KEY_GROUND_NAME, null)?.trim().orEmpty()
        val groundLocation = prefs.getString(KEY_GROUND_LOCATION, null)?.trim().orEmpty()
        val groundFees = prefs.getFloat(KEY_GROUND_FEES, 0.0f).toDouble()
        val ballProvided = prefs.getBoolean(KEY_BALL_PROVIDED, false)
        val noOfBalls = prefs.getInt(KEY_NO_OF_BALLS, 0)
        val ballName = prefs.getString(KEY_BALL_NAME, null)
        val overs = prefs.getInt(KEY_OVERS, 20)

        if (team1.isBlank() || team2.isBlank() || groundName.isBlank() || groundLocation.isBlank()) return null

        return UpcomingMatch(
            dateUtcMillis = date,
            team1 = team1,
            team2 = team2,
            groundName = groundName,
            groundLocation = groundLocation,
            groundFees = groundFees,
            ballProvided = ballProvided,
            noOfBalls = noOfBalls,
            ballName = ballName,
            overs = overs
        )
    }

    fun save(context: Context, match: UpcomingMatch) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_DATE_UTC_MILLIS, match.dateUtcMillis)
            .putString(KEY_TEAM1, match.team1.trim())
            .putString(KEY_TEAM2, match.team2.trim())
            .putString(KEY_GROUND_NAME, match.groundName.trim())
            .putString(KEY_GROUND_LOCATION, match.groundLocation.trim())
            .putFloat(KEY_GROUND_FEES, match.groundFees.toFloat())
            .putBoolean(KEY_BALL_PROVIDED, match.ballProvided)
            .putInt(KEY_NO_OF_BALLS, match.noOfBalls)
            .putString(KEY_BALL_NAME, match.ballName)
            .putInt(KEY_OVERS, match.overs)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
