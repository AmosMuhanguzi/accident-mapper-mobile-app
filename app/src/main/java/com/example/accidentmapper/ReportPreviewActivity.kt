package com.example.accidentmapper

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.accidentmapper.database.DatabaseHandler
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.util.*

class ReportPreviewActivity : AppCompatActivity() {

    private lateinit var dbHandler: DatabaseHandler
    private var mediaUri: Uri? = null
    private var mediaType: String = "Media" // 🔑 NEW: Store the passed media type
    private var submitterName: String = "Unknown"
    private var submitterEmail: String = "unknown@example.com"
    private var submitterProfileUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_preview)

        dbHandler = DatabaseHandler(this)

        // 1. Get data from Intent (passed from SelectFromGalleryActivity)
        val uriString = intent.getStringExtra(SelectFromGalleryActivity.MEDIA_URI_EXTRA)
        submitterName = intent.getStringExtra(SelectFromGalleryActivity.REPORTER_NAME_EXTRA) ?: submitterName
        submitterEmail = intent.getStringExtra(SelectFromGalleryActivity.REPORTER_EMAIL_EXTRA) ?: submitterEmail
        submitterProfileUrl = intent.getStringExtra(SelectFromGalleryActivity.REPORTER_PROFILE_URL_EXTRA)
        mediaType = intent.getStringExtra(SelectFromGalleryActivity.MEDIA_TYPE_EXTRA) ?: "Media" // 🔑 RECEIVE MEDIA TYPE

        if (uriString != null) {
            mediaUri = Uri.parse(uriString)
            displayMedia(mediaUri!!) // 🔑 Removed mimeType check, use stored mediaType
        } else {
            Toast.makeText(this, "Error: No media selected.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        val btnSubmit = findViewById<Button>(R.id.btn_preview_submit)
        val etDetails = findViewById<EditText>(R.id.et_preview_details)
        val tvName = findViewById<TextView>(R.id.tv_reporter_name)
        val ivProfile = findViewById<CircleImageView>(R.id.iv_reporter_profile)

        // Set Reporter Info
        tvName.text = submitterName
        if (submitterProfileUrl != null) {
            Picasso.get().load(submitterProfileUrl)
                .placeholder(R.drawable.ic_user_placeholder)
                .error(R.drawable.ic_user_placeholder)
                .into(ivProfile)
        } else {
            ivProfile.setImageResource(R.drawable.ic_user_placeholder)
        }

        // Set Submission Listener
        btnSubmit.setOnClickListener {
            submitReport(etDetails.text.toString().trim())
        }
    }

    // 🔑 UPDATED: Use the mediaType property instead of re-calculating mimeType
    private fun displayMedia(uri: Uri) {
        val ivMedia = findViewById<ImageView>(R.id.iv_media_preview)

        if (mediaType == "Video") {
            // Attempt to show video thumbnail
            showVideoThumbnail(uri, ivMedia)
        } else {
            // 🔑 FIX: Use Picasso for reliable Image loading from content URIs
            Picasso.get()
                .load(uri)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_error)
                .into(ivMedia)
        }
    }

    private fun showVideoThumbnail(uri: Uri, ivMedia: ImageView) {
        // Ensure Log tag matches for easier filtering
        val LOG_TAG = "ReportPreview"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this@ReportPreviewActivity, uri)
                val bitmap = retriever.getFrameAtTime(1000) // 1 second mark
                retriever.release()

                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        ivMedia.setImageBitmap(bitmap)
                    } else {
                        Log.w(LOG_TAG, "Failed to generate video thumbnail. Bitmap was null.")
                        // Fallback to generic icon if thumbnail generation fails
                        ivMedia.setImageResource(R.drawable.ic_video_placeholder)
                    }
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error generating thumbnail for URI: ${uri.toString()}", e)
                withContext(Dispatchers.Main) {
                    // Fallback: show generic icon
                    ivMedia.setImageResource(R.drawable.ic_video_placeholder)
                }
            }
        }
    }


    private fun submitReport(details: String) {
        if (mediaUri == null || details.isEmpty()) {
            Toast.makeText(this, "Please provide media and a description.", Toast.LENGTH_SHORT).show()
            return
        }

        val btnSubmit = findViewById<Button>(R.id.btn_preview_submit)
        btnSubmit.isEnabled = false

        val newReport = AccidentReport(
            id = UUID.randomUUID().toString(),
            description = details,
            mediaUri = mediaUri.toString(),
            reportType = mediaType, // 🔑 Use the correct, determined mediaType
            timestamp = LocalDateTime.now(),
            reporterName = submitterName,
            reporterEmail = submitterEmail,
            reporterProfileUrl = submitterProfileUrl
        )

        // Database Insertion (Off-Main Thread)
        CoroutineScope(Dispatchers.IO).launch {
            val rowId = dbHandler.addReport(newReport)

            withContext(Dispatchers.Main) {
                if (rowId != -1L) {
                    Toast.makeText(this@ReportPreviewActivity, "Media Report Submitted!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    btnSubmit.isEnabled = true
                    Toast.makeText(this@ReportPreviewActivity, "Submission Failed.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}