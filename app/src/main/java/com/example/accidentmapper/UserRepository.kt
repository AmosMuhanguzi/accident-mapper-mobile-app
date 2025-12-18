package com.example.accidentmapper

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository class that abstracts the data layer, using Room (SQLite).
 */
class UserRepository(context: Context) {

    private val userDao = AppDatabase.getDatabase(context).userDao()
    private val TAG = "UserRepository"

    // Simulates a volatile session state (which user is currently logged in)
    var currentLoggedInEmail: String? = null

    /**
     * Registers a new user and saves them to the SQLite database.
     */
    suspend fun signup(name: String, email: String, region: String, password: String, profileUri: Uri?): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if the user already exists
            if (userDao.getUserByEmail(email) != null) {
                Log.w(TAG, "Signup failed: User $email already exists.")
                return@withContext false
            }

            val profileUrlString = profileUri?.toString() ?: ""
            val newUser = UserEntity(
                name = name,
                email = email,
                region = region,
                passwordHash = password,
                profileImageUrl = profileUrlString
            )

            userDao.insertUser(newUser)
            currentLoggedInEmail = email // Log the user in immediately after signup
            Log.i(TAG, "User $email signed up successfully in SQLite.")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Signup failed during DB operation: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Logs in the user by checking credentials against the SQLite database.
     */
    suspend fun login(email: String, password: String): UserEntity? = withContext(Dispatchers.IO) {
        val user = userDao.getUserByEmail(email)

        if (user == null) {
            return@withContext null
        }

        // Simple password check (remember: hash in production!)
        if (user.passwordHash == password) {
            currentLoggedInEmail = email // Update volatile session state
            Log.i(TAG, "User $email logged in successfully.")
            return@withContext user
        }

        return@withContext null
    }

    /**
     * Retrieves the currently logged-in user from the database.
     */
    suspend fun getCurrentUser(): UserEntity? = withContext(Dispatchers.IO) {
        return@withContext currentLoggedInEmail?.let { userDao.getUserByEmail(it) }
    }

    fun logout() {
        currentLoggedInEmail = null
        Log.i(TAG, "User logged out.")
    }
}