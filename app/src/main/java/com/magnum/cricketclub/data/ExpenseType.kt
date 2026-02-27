package com.magnum.cricketclub.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_types")
data class ExpenseType(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = ""
)
