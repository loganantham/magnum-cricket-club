package com.magnum.cricketclub.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val expenseTypeId: Long? = null, // Used when isIncome = false
    val incomeTypeId: Long? = null, // Used when isIncome = true
    val amount: Double,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val isIncome: Boolean = false // false = expense (subtraction), true = income (addition)
)
