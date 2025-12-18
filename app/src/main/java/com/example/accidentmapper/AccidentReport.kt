package com.example.accidentmapper

import java.io.Serializable
import java.time.LocalDateTime

/**
 * Data class representing a complete accident report, including reporter details.
 * This structure is used for the 1-hour filtering in HomeActivity.kt.
 */
data class AccidentReport(
    val id: String,
    val description: String,
    val mediaUri: String? = null, // URI for image/video or null for text-only
    val reportType: String, // e.g., "Text", "Image", "Video", "Media"
    val timestamp: LocalDateTime,

    // Reporter Data (Used for profile display on the feed card)
    val reporterName: String,
    val reporterEmail: String,
    val reporterProfileUrl: String? // URI/URL to display the user's profile image
) : Serializable
