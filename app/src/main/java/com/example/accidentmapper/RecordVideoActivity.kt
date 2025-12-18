package com.example.accidentmapper

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import com.example.accidentmapper.database.DatabaseHandler
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Explicit imports for clarity (though a wildcard is used above)
import androidx.camera.video.Recorder

class RecordVideoActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var btnRecordVideo: Button
    private lateinit var ivVideoThumbnail: ImageView
    private lateinit var etDetails: EditText
    private lateinit var btnSubmitReport: Button
    private lateinit var tvVideoDuration: TextView
    private lateinit var dbHandler: DatabaseHandler

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private var recordedVideoUri: Uri? = null

    // Constant for the 50 MB limit in bytes
    private val MAX_FILE_BYTES = 50 * 1024 * 1024L

    // User data from Intent
    private var submitterName: String = "Unknown"
    private var submitterEmail: String = "unknown@example.com"
    private var submitterProfileUrl: String? = null

    companion object {
        private const val TAG = "RecordVideoActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_video)

        // 1. Initialize Views and Database
        viewFinder = findViewById(R.id.videoViewFinder)
        btnRecordVideo = findViewById(R.id.btn_record_video)
        ivVideoThumbnail = findViewById(R.id.iv_video_thumbnail)
        etDetails = findViewById(R.id.et_video_details)
        btnSubmitReport = findViewById(R.id.btn_submit_video_report)
        tvVideoDuration = findViewById(R.id.tv_video_duration)
        dbHandler = DatabaseHandler(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 2. Get User Data from Intent
        submitterName = intent.getStringExtra("submitterName") ?: submitterName
        submitterEmail = intent.getStringExtra("submitterEmail") ?: submitterEmail
        submitterProfileUrl = intent.getStringExtra("submitterProfileUrl")

        // 3. Request Permissions and Start Camera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // 4. Set Listeners
        btnRecordVideo.setOnClickListener { captureVideo() }
        btnSubmitReport.setOnClickListener { submitReport() }

        btnSubmitReport.isEnabled = false
    }

    // --- Camera Initialization and Binding ---

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview Use Case
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            // Video Capture Use Case: Removed setMaxFileSize to fix unresolved reference error
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                // 🔑 REMOVED: .setMaxFileSize(maxFileSizeMb)
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Video Camera failed to start.", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // --- Video Recording Logic ---

    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        if (recording != null) {
            // If already recording, stop it
            recording?.stop()
            recording = null
            btnRecordVideo.text = "Start Recording"
            btnSubmitReport.isEnabled = (recordedVideoUri != null)
            return
        }

        // UI State: Hide thumbnail, show viewfinder
        viewFinder.visibility = View.VISIBLE
        ivVideoThumbnail.visibility = View.GONE
        tvVideoDuration.visibility = View.GONE

        // Create file for the video output
        val videoFile = getOutputFile()
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        // Set up video capture listener
        val audioEnabled = (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO))

        recording = videoCapture.output.prepareRecording(this, outputOptions)
            .apply { if (audioEnabled) withAudioEnabled() }
            .start(ContextCompat.getMainExecutor(this), Consumer { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        btnRecordVideo.text = "STOP"
                        btnSubmitReport.isEnabled = false
                        // TODO: Implement timer for real-time duration feedback if required
                    }
                    is VideoRecordEvent.Finalize -> {
                        // Check for errors first
                        if (event.hasError()) {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture failed: ${event.error}")
                            Toast.makeText(baseContext, "Video capture failed. Code: ${event.error}", Toast.LENGTH_LONG).show()

                        } else {
                            // Video recorded successfully, now check file size manually
                            recordedVideoUri = event.outputResults.outputUri

                            val contentResolver = applicationContext.contentResolver
                            val fileSize = try {
                                contentResolver.openFileDescriptor(recordedVideoUri!!, "r")?.statSize ?: 0L
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting file size", e)
                                0L
                            }

                            if (fileSize > MAX_FILE_BYTES) {
                                // File is too large, delete it and report error
                                contentResolver.delete(recordedVideoUri!!, null, null)
                                recordedVideoUri = null
                                Toast.makeText(baseContext, "Recording stopped: Video exceeded 50MB limit ($MAX_FILE_BYTES bytes).", Toast.LENGTH_LONG).show()
                            } else {
                                // File is acceptable and URI is set
                                showVideoPreview(recordedVideoUri)
                                Toast.makeText(baseContext, "Video captured successfully.", Toast.LENGTH_SHORT).show()
                            }
                        }

                        btnRecordVideo.text = "Retake Video"
                        btnSubmitReport.isEnabled = (recordedVideoUri != null) // Enable only if a valid, non-oversized video exists
                    }
                }
            })
    }

    // --- UI Update and Submission ---

    private fun showVideoPreview(uri: Uri?) {
        if (uri == null) return

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            val bitmap = retriever.getFrameAtTime(1000)

            // Get video duration and format it
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMillis = durationStr?.toLongOrNull() ?: 0
            val durationFormatted = formatDuration(durationMillis)
            retriever.release()

            // 2. Update UI
            viewFinder.visibility = View.GONE
            ivVideoThumbnail.visibility = View.VISIBLE

            // Display duration and thumbnail
            tvVideoDuration.text = durationFormatted
            tvVideoDuration.visibility = View.VISIBLE

            if (bitmap != null) {
                ivVideoThumbnail.setImageBitmap(bitmap)
            } else {
                ivVideoThumbnail.setImageResource(R.drawable.ic_video_placeholder)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error showing preview/duration", e)
            viewFinder.visibility = View.GONE
            ivVideoThumbnail.visibility = View.VISIBLE
            ivVideoThumbnail.setImageResource(R.drawable.ic_error)
            tvVideoDuration.visibility = View.GONE
            Toast.makeText(this, "Could not load video preview.", Toast.LENGTH_SHORT).show()
        }
    }

    // NEW HELPER FUNCTION: To format milliseconds to a readable string (e.g., 00:15)
    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun submitReport() {
        val details = etDetails.text.toString().trim()

        if (recordedVideoUri == null) {
            Toast.makeText(this, "Please record a video first.", Toast.LENGTH_SHORT).show()
            return
        }
        if (details.isEmpty()) {
            Toast.makeText(this, "Please describe the accident details.", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmitReport.isEnabled = false

        val currentDateTime = LocalDateTime.now()
        val reportId = UUID.randomUUID().toString()

        val newReport = AccidentReport(
            id = reportId,
            description = details,
            mediaUri = recordedVideoUri.toString(), // Save the URI string
            reportType = "Video", // Set type to Video
            timestamp = currentDateTime,
            reporterName = submitterName,
            reporterEmail = submitterEmail,
            reporterProfileUrl = submitterProfileUrl
        )

        // Database Insertion (Off-Main Thread)
        CoroutineScope(Dispatchers.IO).launch {
            val rowId = dbHandler.addReport(newReport)

            withContext(Dispatchers.Main) {
                if (rowId != -1L) {
                    Toast.makeText(this@RecordVideoActivity, "Video Report Submitted and Stored!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    btnSubmitReport.isEnabled = true
                    Toast.makeText(this@RecordVideoActivity, "Submission Failed.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- Helper Functions and Lifecycle (Unchanged) ---

    private fun getOutputFile(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "AccidentMapperVideos").apply { mkdirs() }
        }
        val outputDirectory = if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir

        val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        return File(outputDirectory, "$timeStamp.mp4") // Save as .mp4
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted. Video recording disabled.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        recording?.close() // Ensure recording resources are released
    }
}