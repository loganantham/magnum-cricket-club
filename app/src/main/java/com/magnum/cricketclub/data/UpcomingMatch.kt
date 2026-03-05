package com.magnum.cricketclub.data

data class UpcomingMatch(
    val dateUtcMillis: Long,
    val team1: String,
    val team2: String,
    val groundName: String,
    val groundLocation: String,
    val groundFees: Double = 0.0,
    val ballProvided: Boolean = false,
    val noOfBalls: Int = 0,
    val ballName: String? = null, // SF Yorker, SF True Test
    val overs: Int = 20
)
