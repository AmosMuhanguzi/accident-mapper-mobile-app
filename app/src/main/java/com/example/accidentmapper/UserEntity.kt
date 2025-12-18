package com.example.accidentmapper

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the 'users' table in the SQLite database.
 */
@Entity(tableName = "users")
data class UserEntity(
    // The email serves as the unique primary key
    @PrimaryKey
    val email: String,
    val name: String,
    val region: String,
    // Note: Storing raw password for simulation. In a production app, this MUST be hashed.
    val passwordHash: String,
    // This holds the local URI string for the profile image
    val profileImageUrl: String
)