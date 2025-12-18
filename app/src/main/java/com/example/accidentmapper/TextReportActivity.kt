package com.example.accidentmapper

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.accidentmapper.AccidentReport
import com.example.accidentmapper.database.DatabaseHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID

class TextReportActivity : AppCompatActivity() {

    private lateinit var etDetails: EditText
    private lateinit var btnSubmit: Button
    private lateinit var dbHandler: DatabaseHandler

    // 🔑 NEW PROPERTIES to hold the actual submitter's data
    private var submitterName: String = ""
    private var submitterEmail: String = ""
    private var submitterProfileUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_report)

        dbHandler = DatabaseHandler(this)
        etDetails = findViewById(R.id.et_report_details)
        btnSubmit = findViewById(R.id.btn_submit_report)

        // 🔑 1. Retrieve actual user data from the Intent
        submitterName = intent.getStringExtra("submitterName") ?: "Unknown User"
        submitterEmail = intent.getStringExtra("submitterEmail") ?: "unknown@example.com"
        submitterProfileUrl = intent.getStringExtra("submitterProfileUrl")

        btnSubmit.setOnClickListener {
            submitReport()
        }
    }

    private fun submitReport() {
        val details = etDetails.text.toString().trim()

        if (details.isEmpty()) {
            Toast.makeText(this, "Please describe the accident details.", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false

        // --- 2. Create Report using Actual User Data ---
        val currentDateTime = LocalDateTime.now()
        val reportId = UUID.randomUUID().toString()

        val newReport = AccidentReport(
            id = reportId,
            description = details,
            mediaUri = null,
            reportType = "Text",
            timestamp = currentDateTime,
            // 🔑 USE ACTUAL DATA HERE:
            reporterName = submitterName,
            reporterEmail = submitterEmail,
            reporterProfileUrl = submitterProfileUrl
        )

        // --- 3. Database Insertion ---
        CoroutineScope(Dispatchers.IO).launch {
            val rowId = dbHandler.addReport(newReport)

            withContext(Dispatchers.Main) {
                btnSubmit.isEnabled = true
                if (rowId != -1L) {
                    Toast.makeText(this@TextReportActivity, "Report Submitted and Stored!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@TextReportActivity, "Submission Failed.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}