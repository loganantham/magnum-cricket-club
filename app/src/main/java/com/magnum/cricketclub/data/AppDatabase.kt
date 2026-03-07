package com.magnum.cricketclub.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Expense::class, ExpenseType::class, IncomeType::class, AppConfig::class, UserProfile::class, ContributionLedgerEntry::class, UpcomingMatch::class],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseTypeDao(): ExpenseTypeDao
    abstract fun incomeTypeDao(): IncomeTypeDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun contributionLedgerDao(): ContributionLedgerDao
    abstract fun upcomingMatchDao(): UpcomingMatchDao
    
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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS contribution_ledger (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        contributorEmail TEXT NOT NULL,
                        year INTEGER NOT NULL,
                        monthIndex INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        pendingAmount REAL NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_contribution_ledger_email_year " +
                            "ON contribution_ledger(contributorEmail, year)"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN createdByEmail TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS upcoming_matches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        dateUtcMillis INTEGER NOT NULL,
                        team1 TEXT NOT NULL,
                        team2 TEXT NOT NULL,
                        groundName TEXT NOT NULL,
                        groundLocation TEXT NOT NULL,
                        groundFees REAL NOT NULL,
                        ballProvided INTEGER NOT NULL,
                        noOfBalls INTEGER NOT NULL,
                        ballName TEXT,
                        overs INTEGER NOT NULL,
                        matchType TEXT NOT NULL,
                        team1FeesCollected INTEGER NOT NULL,
                        team2FeesCollected INTEGER NOT NULL,
                        groundFeesShared INTEGER NOT NULL,
                        team1FeesStatus TEXT NOT NULL,
                        team2FeesStatus TEXT NOT NULL,
                        team1PendingAmount REAL NOT NULL,
                        team2PendingAmount REAL NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add missing userId to user_profile
                if (!columnExists(database, "user_profile", "userId")) {
                    database.execSQL("ALTER TABLE user_profile ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                }
                // Add missing userId to expenses
                if (!columnExists(database, "expenses", "userId")) {
                    database.execSQL("ALTER TABLE expenses ADD COLUMN userId TEXT")
                }
                // Add missing lastModified and isDeleted to upcoming_matches
                if (!columnExists(database, "upcoming_matches", "lastModified")) {
                    database.execSQL("ALTER TABLE upcoming_matches ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
                }
                if (!columnExists(database, "upcoming_matches", "isDeleted")) {
                    database.execSQL("ALTER TABLE upcoming_matches ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                }
            }

            private fun columnExists(database: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
                val cursor = database.query("PRAGMA table_info($tableName)")
                cursor.use {
                    val nameIndex = it.getColumnIndex("name")
                    if (nameIndex == -1) return false
                    while (it.moveToNext()) {
                        if (it.getString(nameIndex) == columnName) {
                            return true
                        }
                    }
                }
                return false
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cricket_expense_database"
                )
                .addMigrations(
                    MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, 
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
                    MIGRATION_8_9
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
