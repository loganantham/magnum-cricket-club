package com.magnum.cricketclub.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Expense::class, ExpenseType::class, IncomeType::class, AppConfig::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseTypeDao(): ExpenseTypeDao
    abstract fun incomeTypeDao(): IncomeTypeDao
    abstract fun appConfigDao(): AppConfigDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cricket_expense_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
