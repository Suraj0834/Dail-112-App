package com.dial112.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * UserEntity - Local cached user session data
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,         // "citizen" or "police"
    val badgeId: String? = null,
    val profileImage: String? = null,
    val station: String? = null,
    val jwtToken: String,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * CaseEntity - Cached FIR/Case for offline viewing
 */
@Entity(tableName = "cases")
data class CaseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val status: String,
    val imageUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: String,
    val updatedAt: String,
    val timelineJson: String,           // JSON-serialized list of TimelineDto
    val assignedOfficerJson: String?    // JSON-serialized OfficerDto
)

/**
 * SosLogEntity - Local SOS log for history
 */
@Entity(tableName = "sos_logs")
data class SosLogEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false  // true once sent to server
)

/**
 * HotspotEntity - Cached crime hotspot data
 */
@Entity(tableName = "hotspots")
data class HotspotEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val riskScore: Double,
    val crimeCount: Int,
    val area: String,
    val cachedAt: Long = System.currentTimeMillis()
)
