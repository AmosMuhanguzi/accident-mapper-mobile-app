package com.example.accidentmapper

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.accidentmapper.database.DatabaseHandler
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureImageActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var btnCaptureImage: Button
    private lateinit var ivImagePreview: ImageView
    private lateinit var etDetails: EditText
    private lateinit var btnSubmitReport: Button
    private lateinit var dbHandler: DatabaseHandler

    // CameraX variables
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var capturedImageUri: Uri? = null

    // User data from Intent (Crucial for submission)
    private var submitterName: String = "Unknown"
    private var submitterEmail: String = "unknown@example.com"
    private var submitterProfileUrl: String? = null

    companion object {
        private const val TAG = "CaptureImageActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        // Request Camera permission is usually enough for modern Android
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_image)

        // 1. Initialize Views and Database
        viewFinder = findViewById(R.id.viewFinder)
        btnCaptureImage = findViewById(R.id.btn_capture_image)
        ivImagePreview = findViewById(R.id.iv_image_preview)
        etDetails = findViewById(R.id.et_report_details)
        btnSubmitReport = findViewById(R.id.btn_submit_report)
        dbHandler = DatabaseHandler(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 2. Get User Data from Intent
        submitterName = intent.getStringExtra("submitterName") ?: submitterName
        submitterEmail = intent.getStringExtra("submitterEmail") ?: submitterEmail
        submitterProfileUrl = intent.getStringExtra("submitterProfileUrl")

        // 3. Request Camera Permissions and Start Camera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // 4. Set Listeners
        btnCaptureImage.setOnClickListener {
            // If the camera is running, capture. If not, try starting it again.
            if (imageCapture != null) {
                takePhoto()
            } else {
                Toast.makeText(this, "Camera not ready, ensure permissions are granted.", Toast.LENGTH_SHORT).show()
                // Re-attempting to start camera if the button is clicked and it failed previously
                if (allPermissionsGranted()) {
                    startCamera()
                }
            }
        }
        btnSubmitReport.setOnClickListener { submitReport() }

        // Initially hide submit button until image is captured
        btnSubmitReport.isEnabled = false
    }

    // --- Camera Initialization and Binding ---

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // Image Capture: Added AspectRatio for better compatibility
            val screenAspectRatio = AspectRatio.RATIO_4_3
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(screenAspectRatio) // Fix added
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                Log.d(TAG, "Camera bound successfully.")

            } catch (exc: Exception) {
                // This catch block handles binding failures (e.g., if camera is busy)
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Camera failed to start: ${exc.message}", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // --- Photo Capture and File Saving ---

    /** Creates a file in the app's external pictures directory for safe storage. */
    private fun getOutputFile(): File {
        // Use getExternalFilesDir(null) as a base, then append a custom subdirectory
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "AccidentMapperImages").apply { mkdirs() }
        }

        // Use the internal file directory as a fallback if external storage fails
        val outputDirectory = if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir

        val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        return File(outputDirectory, "$timeStamp.jpg")
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: run {
            Toast.makeText(this, "Camera is not ready. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create time-stamped file for output using the safer helper
        val photoFile = getOutputFile()

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(baseContext, "Photo capture failed: ${exc.message}", Toast.LENGTH_LONG).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturedImageUri = output.savedUri

                    // 1. Update UI to show preview
                    viewFinder.visibility = View.GONE
                    btnCaptureImage.text = "Retake Photo"
                    ivImagePreview.visibility = View.VISIBLE
                    btnSubmitReport.isEnabled = true

                    // 2. Load preview image
                    Picasso.get().load(capturedImageUri).into(ivImagePreview)
                    Log.d(TAG, "Photo saved successfully: $capturedImageUri")
                }
            })
    }

    // --- Report Submission ---

    private fun submitReport() {
        val details = etDetails.text.toString().trim()

        if (capturedImageUri == null) {
            Toast.makeText(this, "Please capture an image first.", Toast.LENGTH_SHORT).show()
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
            mediaUri = capturedImageUri.toString(), // Save the URI string
            reportType = "Image",
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
                    Toast.makeText(this@CaptureImageActivity, "Image Report Submitted and Stored!", Toast.LENGTH_LONG).show()
                    finish() // Return to HomeActivity
                } else {
                    btnSubmitReport.isEnabled = true
                    Toast.makeText(this@CaptureImageActivity, "Submission Failed.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- Permissions and Lifecycle ---

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera() // Start camera immediately after permission is granted
            } else {
                Toast.makeText(this, "Permissions not granted by the user. Camera cannot be used.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}