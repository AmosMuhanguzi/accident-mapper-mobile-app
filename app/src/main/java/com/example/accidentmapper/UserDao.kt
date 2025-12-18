package com.example.accidentmapper

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Interface defining all SQL operations for the User table.
 */
@Dao
interface UserDao {
    // Inserts a user. If the email already exists, it replaces the old entry (for updates).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    // Retrieves a user by their unique email
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?
}