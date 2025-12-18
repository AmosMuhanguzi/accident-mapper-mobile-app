package com.example.accidentmapper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class SelectFromGalleryActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GallerySelect"
        const val MEDIA_URI_EXTRA = "media_uri"
        const val REPORTER_NAME_EXTRA = "reporter_name"
        const val REPORTER_EMAIL_EXTRA = "reporter_email"
        const val REPORTER_PROFILE_URL_EXTRA = "reporter_profile_url"
        const val MEDIA_TYPE_EXTRA = "media_type" // Constant to pass media type
    }

    private lateinit var submitterName: String
    private lateinit var submitterEmail: String
    private var submitterProfileUrl: String? = null

    private val selectMediaLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                Log.d(TAG, "Media selected successfully: $uri")

                // 🔑 FIX 1: Grant persistent read access to prevent SecurityException
                val takeFlags: Int = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try {
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Failed to get persistent permission, check URI support.", e)
                }

                // 🔑 FIX 2: Determine and save Media Type
                val mimeType = contentResolver.getType(uri)
                val mediaType = when {
                    mimeType?.startsWith("video/") == true -> "Video"
                    mimeType?.startsWith("image/") == true -> "Image"
                    else -> "Media"
                }

                launchReportPreview(uri.toString(), mediaType)
            } ?: run {
                Log.e(TAG, "Result OK but no URI data found.")
                Toast.makeText(this, "Failed to retrieve media.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Log.d(TAG, "Media selection cancelled.")
            Toast.makeText(this, "Media selection cancelled.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve user data passed from HomeActivity
        submitterName = intent.getStringExtra(REPORTER_NAME_EXTRA) ?: "Unknown"
        submitterEmail = intent.getStringExtra(REPORTER_EMAIL_EXTRA) ?: "unknown@example.com"
        submitterProfileUrl = intent.getStringExtra(REPORTER_PROFILE_URL_EXTRA)

        // Automatically start gallery selection when activity is created
        selectMediaFromGallery()
    }

    private fun selectMediaFromGallery() {
        // Using ACTION_GET_CONTENT with EXTRA_MIME_TYPES to reliably open media gallery
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            // Set primary type to image/* and put video/* in the extra list
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        }

        if (intent.resolveActivity(packageManager) != null) {
            Log.d(TAG, "Launching gallery intent.")
            selectMediaLauncher.launch(intent)
        } else {
            val errorMessage = "Error: No app found to select media. Check device configuration."
            Log.e(TAG, errorMessage)
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // NEW: Added mediaType parameter
    private fun launchReportPreview(uriString: String, mediaType: String) {
        val intent = Intent(this, ReportPreviewActivity::class.java).apply {
            putExtra(MEDIA_URI_EXTRA, uriString)
            putExtra(REPORTER_NAME_EXTRA, submitterName)
            putExtra(REPORTER_EMAIL_EXTRA, submitterEmail)
            putExtra(REPORTER_PROFILE_URL_EXTRA, submitterProfileUrl)
            putExtra(MEDIA_TYPE_EXTRA, mediaType) // Pass the determined type
        }
        startActivity(intent)
        finish()
    }
}