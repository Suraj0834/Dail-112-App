package com.dial112.domain.model

/**
 * Domain Models - Clean Architecture domain layer
 * These are the pure business entities, independent of data/UI layers.
 */

/**
 * User - Domain model for authenticated user
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val badgeId: String? = null,
    val profileImage: String? = null,
    val station: String? = null
)

/**
 * UserRole - Enum for role-based access control
 */
enum class UserRole(val value: String) {
    CITIZEN("citizen"),
    POLICE("police");

    companion object {
        fun fromString(value: String) = values().find { it.value == value } ?: CITIZEN
    }
}

/**
 * Case - Domain model for FIR / complaint
 */
data class Case(
    val id: String,
    val title: String,
    val description: String,
    val category: CaseCategory,
    val status: CaseStatus,
    val imageUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: String,
    val updatedAt: String,
    val timeline: List<TimelineEntry>,
    val assignedOfficer: Officer?
)

/**
 * CaseStatus - Enum for case progress
 */
enum class CaseStatus(val displayName: String) {
    PENDING("Pending"),
    INVESTIGATING("Investigating"),
    RESOLVED("Resolved"),
    CLOSED("Closed")
}

/**
 * CaseCategory - Enum for complaint categories
 */
enum class CaseCategory(val displayName: String) {
    THEFT("Theft"),
    CYBERCRIME("Cybercrime"),
    VIOLENCE("Violence"),
    FRAUD("Fraud"),
    HARASSMENT("Harassment"),
    ACCIDENT("Accident"),
    OTHER("Other")
}

/**
 * TimelineEntry - Case progress entry
 */
data class TimelineEntry(
    val status: String,
    val note: String,
    val timestamp: String
)

/**
 * Officer - Domain model for police officer
 */
data class Officer(
    val id: String,
    val name: String,
    val badgeId: String,
    val phone: String,
    val profileImage: String?,
    val station: String,
    val distance: Double? = null
)

/**
 * SosEmergency - Domain model for SOS alert
 */
data class SosEmergency(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val type: String,
    val nearestOfficer: Officer?
)

/**
 * Vehicle - Domain model for vehicle lookup
 */
data class Vehicle(
    val id: String,
    val plateNumber: String,
    val ownerName: String,
    val ownerPhone: String,
    val vehicleType: String,
    val model: String,
    val color: String,
    val isStolen: Boolean,
    val isSuspected: Boolean
)

/**
 * Hotspot - Domain model for crime hotspot
 */
data class Hotspot(
    val latitude: Double,
    val longitude: Double,
    val riskScore: Double,
    val crimeCount: Int,
    val area: String
)

/**
 * FaceMatch - Domain model for face recognition result
 */
data class FaceMatch(
    val matched: Boolean,
    val criminalId: String?,
    val name: String?,
    val confidence: Double?,
    val crimeHistory: List<String>?
)

/**
 * WeaponDetection - Domain model for weapon detection
 */
data class WeaponDetection(
    val weaponDetected: Boolean,
    val detections: List<Detection>
)

/**
 * Detection - Single weapon bounding box
 */
data class Detection(
    val label: String,
    val confidence: Double,
    val bbox: List<Float>
)

/**
 * ComplaintCategory - NLP classification result
 */
data class ComplaintCategory(
    val category: String,
    val confidence: Double,
    val allScores: Map<String, Double>
)

/**
 * PcrVan - Domain model for PCR (Police Control Room) van
 */
data class PcrVan(
    val id: String,
    val vehicleName: String,
    val plateNo: String,
    val model: String,
    val color: String,
    val station: String,
    val status: String,           // Available, Busy, Off-Duty, Maintenance
    val assignedOfficer: Officer?,
    val coDriver: Officer?,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val lastSeen: String?,
    val notes: String
)

/**
 * SosUser - Minimal user info embedded in SOS logs
 */
data class SosUser(
    val id: String,
    val name: String,
    val phone: String
)

/**
 * SosLog - Full SOS emergency log from server
 */
data class SosLog(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val type: String,
    val status: String,           // ACTIVE, RESPONDED, RESOLVED, FALSE_ALARM
    val triggeredBy: SosUser?,    // populated for police view
    val respondingOfficer: Officer?,
    val responseTimeSeconds: Int?,
    val resolvedAt: String?,
    val createdAt: String
)

/**
 * CrimeHistoryEntry - Single offense record within a criminal profile
 */
data class CrimeHistoryEntry(
    val offense: String,
    val date: String?,
    val status: String?
)

/**
 * Criminal - Domain model for criminal database entry
 */
data class Criminal(
    val id: String,
    val name: String,
    val age: Int?,
    val gender: String?,
    val dangerLevel: String,      // LOW, MEDIUM, HIGH, CRITICAL
    val lastKnownAddress: String?,
    val photo: String?,
    val warrantStatus: Boolean,
    val isActive: Boolean,
    val crimeHistory: List<CrimeHistoryEntry>,
    val hasEmbedding: Boolean,
    val description: String?
)

/**
 * Resource - Sealed class for UI state management (Loading, Success, Error)
 */
sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val code: Int? = null) : Resource<Nothing>()
}
