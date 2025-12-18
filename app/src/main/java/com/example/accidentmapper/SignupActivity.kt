package com.example.accidentmapper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private lateinit var userRepository: UserRepository

    private lateinit var inputName: TextInputEditText
    private lateinit var inputEmail: TextInputEditText
    private lateinit var inputRegion: TextInputEditText
    private lateinit var inputPassword: TextInputEditText
    private lateinit var inputConfirmPassword: TextInputEditText
    private lateinit var profilePreview: CircleImageView
    private lateinit var btnSelectPhoto: Button
    private lateinit var btnRemovePhoto: Button
    private lateinit var signupButton: Button
    private lateinit var txtLogin: TextView // The "Already have an account" link

    companion object {
        const val PICK_IMAGE_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize UserRepository for Room access
        userRepository = UserRepository(applicationContext)

        // Initialize views
        inputName = findViewById(R.id.inputName)
        inputEmail = findViewById(R.id.inputEmail)
        inputRegion = findViewById(R.id.inputRegion)
        inputPassword = findViewById(R.id.inputPassword)
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword)
        profilePreview = findViewById(R.id.profilePreview)
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto)
        signupButton = findViewById(R.id.signupButton)
        txtLogin = findViewById(R.id.loginText)

        // Make the "Already have an account? Login" link work
        txtLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Select profile photo
        btnSelectPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { // Changed to ACTION_OPEN_DOCUMENT for better URI persistence
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                // Add the flag to ensure the content provider grants access permissions
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Remove photo
        btnRemovePhoto.setOnClickListener {
            selectedImageUri?.let { uri ->
                // Revoke permission when photo is removed
                contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            selectedImageUri = null
            profilePreview.setImageResource(R.drawable.ic_user_placeholder)
            btnRemovePhoto.visibility = Button.GONE
        }

        // Signup button
        signupButton.setOnClickListener {
            val name = inputName.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val region = inputRegion.text.toString().trim()
            val password = inputPassword.text.toString().trim()
            val confirmPassword = inputConfirmPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || region.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()
            ) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Use CoroutineScope to call suspend function for DB access
            CoroutineScope(Dispatchers.Main).launch {
                handleSignup(name, email, region, password, selectedImageUri)
            }
        }
    }

    private suspend fun handleSignup(name: String, email: String, region: String, password: String, imageUri: Uri?) {
        val success = userRepository.signup(name, email, region, password, imageUri)

        if (success) {
            Toast.makeText(this, "Signup successful! You are now logged in.", Toast.LENGTH_LONG).show()

            // Navigate to HomeActivity, passing data just saved
            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra("userName", name)
                putExtra("userEmail", email)
                putExtra("userRegion", region)
                putExtra("userProfileUrl", imageUri?.toString() ?: "")
            }
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Signup failed: A user with this email already exists.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data

            // ⭐ CRITICAL FIX: Take persistent read permission for the URI
            selectedImageUri?.let { uri ->
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                // We must ensure the URI is supported by persistable permissions
                try {
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: SecurityException) {
                    // Handle cases where the URI provider doesn't support persistence (rare)
                    Toast.makeText(this, "Warning: Could not get permanent image permission.", Toast.LENGTH_LONG).show()
                }
            }
            // ⭐ END CRITICAL FIX

            profilePreview.setImageURI(selectedImageUri)
            btnRemovePhoto.visibility = Button.VISIBLE
        }
    }
}