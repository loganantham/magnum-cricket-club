package com.magnum.cricketclub.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Expense::class, ExpenseType::class, IncomeType::class, AppConfig::class, UserProfile::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseTypeDao(): ExpenseTypeDao
    abstract fun incomeTypeDao(): IncomeTypeDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun userProfileDao(): UserProfileDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_profile (
                        email TEXT NOT NULL PRIMARY KEY,
                        playerPreference TEXT,
                        mobileNumber TEXT,
                        alternateMobileNumber TEXT
                    )
                """.trimIndent())
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN name TEXT")
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN additionalResponsibility TEXT")
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cricket_expense_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
