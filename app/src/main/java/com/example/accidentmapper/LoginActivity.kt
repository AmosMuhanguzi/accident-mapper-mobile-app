package com.example.accidentmapper

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var userRepository: UserRepository
    private lateinit var inputEmail: TextInputEditText
    private lateinit var inputPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var txtSignup: TextView
    private lateinit var txtForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize UserRepository for Room access
        userRepository = UserRepository(applicationContext)

        // Initialize views
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        btnLogin = findViewById(R.id.loginButton)
        txtSignup = findViewById(R.id.signupText)
        txtForgotPassword = findViewById(R.id.forgotPasswordButton)

        btnLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Use CoroutineScope to call suspend function
            CoroutineScope(Dispatchers.Main).launch {
                loginUser(email, password)
            }
        }

        txtSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        txtForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private suspend fun loginUser(email: String, password: String) {
        val user = userRepository.login(email, password)

        if (user != null) {
            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

            // Pass user data retrieved directly from the SQLite database to HomeActivity
            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra("userName", user.name)
                putExtra("userEmail", user.email)
                putExtra("userRegion", user.region)
                putExtra("userProfileUrl", user.profileImageUrl)
            }
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Login failed: Invalid credentials or user not found.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val emailInput = dialogView.findViewById<TextInputEditText>(R.id.inputResetEmail)
        val btnSend = dialogView.findViewById<Button>(R.id.btnSendResetLink)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnSend.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                // Check if user exists in the local database before simulating reset
                CoroutineScope(Dispatchers.Main).launch {
                    val user = userRepository.login(email, "placeholder_check") // Check for email existence
                    if (user != null) {
                        Toast.makeText(this@LoginActivity, "Simulated: Reset link sent to $email", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this@LoginActivity, "Error: Email not registered.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
}