package com.magnum.cricketclub.data

class ContributionLedgerRepository(
    private val contributionLedgerDao: ContributionLedgerDao
) {
    suspend fun getEntriesForContributorAndYear(
        email: String,
        year: Int
    ): List<ContributionLedgerEntry> {
        return contributionLedgerDao.getEntriesForContributorAndYear(email, year)
    }

    suspend fun upsertEntry(
        email: String,
        year: Int,
        monthIndex: Int,
        status: String,
        pendingAmount: Double
    ): ContributionLedgerEntry {
        val existingForYear = contributionLedgerDao.getEntriesForContributorAndYear(email, year)
        val existingForMonth = existingForYear.firstOrNull { it.monthIndex == monthIndex }

        val entry = if (existingForMonth != null) {
            existingForMonth.copy(
                status = status,
                pendingAmount = pendingAmount
            )
        } else {
            ContributionLedgerEntry(
                contributorEmail = email,
                year = year,
                monthIndex = monthIndex,
                status = status,
                pendingAmount = pendingAmount
            )
        }

        contributionLedgerDao.insertOrReplace(entry)
        return entry
    }
}

