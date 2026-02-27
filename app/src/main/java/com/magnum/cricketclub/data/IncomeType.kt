package com.magnum.cricketclub.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_types")
data class IncomeType(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = ""
)
