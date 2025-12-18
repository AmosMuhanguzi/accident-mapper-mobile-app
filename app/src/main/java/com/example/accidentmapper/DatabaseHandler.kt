package com.example.accidentmapper.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.accidentmapper.AccidentReport
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class DatabaseHandler(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // Database Metadata
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "AccidentMapperDB"
        private const val TAG = "DatabaseHandler"

        // Reports Table Constants
        private const val TABLE_REPORTS = "accident_reports"
        private const val KEY_ID = "id"
        private const val KEY_DETAILS = "description"
        private const val KEY_MEDIA_URI = "media_uri"
        private const val KEY_REPORT_TYPE = "report_type"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_REPORTER_NAME = "reporter_name"
        private const val KEY_REPORTER_EMAIL = "reporter_email"
        private const val KEY_PROFILE_URL = "reporter_profile_url"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // SQL statement to create the Accident Reports table
        val CREATE_REPORTS_TABLE = ("CREATE TABLE $TABLE_REPORTS ("
                + "$KEY_ID TEXT PRIMARY KEY,"
                + "$KEY_DETAILS TEXT,"
                + "$KEY_MEDIA_URI TEXT,"
                + "$KEY_REPORT_TYPE TEXT,"
                + "$KEY_TIMESTAMP INTEGER," // Stored as Unix Milliseconds
                + "$KEY_REPORTER_NAME TEXT,"
                + "$KEY_REPORTER_EMAIL TEXT,"
                + "$KEY_PROFILE_URL TEXT,"
                + "$KEY_LATITUDE REAL,"
                + "$KEY_LONGITUDE REAL" + ")")
        db.execSQL(CREATE_REPORTS_TABLE)
        Log.d(TAG, "Database table '$TABLE_REPORTS' created.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This drops the table and recreates it, wiping all old data.
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REPORTS")
        onCreate(db)
        Log.d(TAG, "Database upgraded from version $oldVersion to $newVersion. Table dropped and recreated.")
    }

    // --- C - CREATE (Insert) Operation ---
    /** Inserts a new AccidentReport into the database. */
    fun addReport(report: AccidentReport): Long {
        val db = this.writableDatabase

        // Convert Kotlin's LocalDateTime to milliseconds since epoch for SQLite storage
        val timestampLong = report.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val contentValues = ContentValues().apply {
            put(KEY_ID, report.id)
            put(KEY_DETAILS, report.description)
            put(KEY_MEDIA_URI, report.mediaUri)
            put(KEY_REPORT_TYPE, report.reportType)
            put(KEY_TIMESTAMP, timestampLong) // Store as Long
            put(KEY_REPORTER_NAME, report.reporterName)
            put(KEY_REPORTER_EMAIL, report.reporterEmail)
            put(KEY_PROFILE_URL, report.reporterProfileUrl)
            // Using placeholder values for location as per simplified requirements
            put(KEY_LATITUDE, 0.0)
            put(KEY_LONGITUDE, 0.0)
        }

        // Inserting row. Returns the row ID or -1 if an error occurred.
        val success = db.insert(TABLE_REPORTS, null, contentValues)
        db.close()
        Log.i(TAG, "Report added with ID: ${report.id}. Success code: $success")
        return success
    }

    // --- R - READ (Retrieve All) Operation ---
    /** Retrieves ALL reports from the database, sorted newest first. Used by HomeActivity for filtering. */
    fun getAllReportsForFiltering(): List<AccidentReport> {
        val reportList = mutableListOf<AccidentReport>()
        val selectQuery = "SELECT * FROM $TABLE_REPORTS ORDER BY $KEY_TIMESTAMP DESC"

        val db = this.readableDatabase
        var cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                // Convert milliseconds back to LocalDateTime
                val timestampLong = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP))
                val reportDateTime = Instant.ofEpochMilli(timestampLong)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()

                val report = AccidentReport(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DETAILS)),
                    mediaUri = cursor.getString(cursor.getColumnIndexOrThrow(KEY_MEDIA_URI)),
                    reportType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_REPORT_TYPE)),
                    timestamp = reportDateTime, // Retrieved LocalDateTime
                    reporterName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_REPORTER_NAME)),
                    reporterEmail = cursor.getString(cursor.getColumnIndexOrThrow(KEY_REPORTER_EMAIL)),
                    reporterProfileUrl = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROFILE_URL))
                )
                reportList.add(report)
            } while (cursor.moveToNext())
        }

        // Critical: Ensure the cursor is closed to prevent resource leaks
        cursor.close()
        db.close()
        Log.i(TAG, "Retrieved ${reportList.size} reports from the database.")
        return reportList
    }
}