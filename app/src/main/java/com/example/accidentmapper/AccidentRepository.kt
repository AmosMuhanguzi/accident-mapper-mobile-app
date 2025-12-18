package com.example.accidentmapper

import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

/**
 * Singleton object to manage the accident reports list in memory.
 * In a real application, this would be an interface implemented by Room or Firebase DAO.
 */
object AccidentRepository {

    // In-memory store for reports.
    private val allReports = mutableListOf<AccidentReport>()
    private const val RECENT_DURATION_HOURS = 1L

    /**
     * Simulates saving a report and its details.
     */
    fun saveReport(
        description: String,
        mediaUri: String?,
        reportType: String,
        reporterName: String,
        reporterEmail: String,
        reporterProfileUrl: String?
    ) {
        val newReport = AccidentReport(
            id = UUID.randomUUID().toString(),
            description = description,
            mediaUri = mediaUri,
            reportType = reportType,
            timestamp = LocalDateTime.now(),
            reporterName = reporterName,
            reporterEmail = reporterEmail,
            reporterProfileUrl = reporterProfileUrl
        )
        // Add to the front to be the most recent
        allReports.add(0, newReport)
    }

    /**
     * Gets all reports, splitting them into Recent (last 1 hour) and Earlier.
     * @return Pair<Recent Reports, Earlier Reports>
     */
    fun getSplitReports(): Pair<List<AccidentReport>, List<AccidentReport>> {
        val now = LocalDateTime.now()
        val recentReports = mutableListOf<AccidentReport>()
        val earlierReports = mutableListOf<AccidentReport>()

        for (report in allReports) {
            val duration = Duration.between(report.timestamp, now)
            // If the report is less than 1 hour old, it's recent
            if (duration.toHours() < RECENT_DURATION_HOURS) {
                recentReports.add(report)
            } else {
                earlierReports.add(report)
            }
        }
        return Pair(recentReports, earlierReports)
    }

    fun refreshReports(): Pair<List<AccidentReport>, List<AccidentReport>> {
        return getSplitReports()
    }
}