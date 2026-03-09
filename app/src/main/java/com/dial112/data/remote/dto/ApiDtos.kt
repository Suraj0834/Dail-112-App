package com.dial112.data.remote.dto

import com.google.gson.annotations.SerializedName

// =============================================================================
// REQUEST DTOs - Data Transfer Objects for API Requests
// =============================================================================

/**
 * RegisterRequest - Body for POST /api/auth/register
 */
data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("role") val role: String,       // "citizen" or "police"
    @SerializedName("badgeId") val badgeId: String? = null  // Police only
)

/**
 * LoginRequest - Body for POST /api/auth/login
 */
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

/**
 * ForgotPasswordRequest - Body for POST /api/auth/forgot-password
 */
data class ForgotPasswordRequest(
    @SerializedName("email") val email: String
)

/**
 * ResetPasswordRequest - Body for POST /api/auth/reset-password
 */
data class ResetPasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("newPassword") val newPassword: String
)

/**
 * MessageResponse - Generic success/message response
 */
data class MessageResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)

/**
 * SosRequest - Body for POST /api/sos
 */
data class SosRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("address") val address: String,
    @SerializedName("type") val type: String = "SOS" // SOS, ACCIDENT, FIRE
)

/**
 * UpdateCaseRequest - Body for PUT /api/cases/:id
 */
data class UpdateCaseRequest(
    @SerializedName("status") val status: String,   // PENDING, INVESTIGATING, RESOLVED
    @SerializedName("notes") val notes: String? = null
)

/**
 * ComplaintClassifyRequest - Body for POST /api/ai/classify-complaint
 */
data class ComplaintClassifyRequest(
    @SerializedName("text") val text: String
)

/**
 * ChatRequest - Body for AI chatbot
 */
data class ChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("sessionId") val sessionId: String
)

// =============================================================================
// RESPONSE DTOs - Data Transfer Objects for API Responses
// =============================================================================

/**
 * AuthResponse - Response from login/register
 */
data class AuthResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("token") val token: String?,
    @SerializedName("user") val user: UserDto?,
    @SerializedName("message") val message: String?
)

/**
 * UserDto - User data from server
 */
data class UserDto(
    @SerializedName("_id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("role") val role: String,
    @SerializedName("badgeId") val badgeId: String? = null,
    @SerializedName("profileImage") val profileImage: String? = null,
    @SerializedName("station") val station: String? = null
)

/**
 * SosResponse - Response from SOS trigger
 */
data class SosResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("sosId") val sosId: String?,
    @SerializedName("nearestOfficer") val nearestOfficer: OfficerDto?
)

/**
 * OfficerDto - Police officer response data
 */
data class OfficerDto(
    @SerializedName("_id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("badgeId") val badgeId: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("profileImage") val profileImage: String?,
    @SerializedName("station") val station: String,
    @SerializedName("distance") val distance: Double?
)

/**
 * CaseResponse - Single FIR/Case response
 */
data class CaseResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("case") val case: CaseDto?,
    @SerializedName("message") val message: String?
)

/**
 * CasesListResponse - List of cases
 */
data class CasesListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("cases") val cases: List<CaseDto>,
    @SerializedName("total") val total: Int
)

/**
 * CaseDto - Case/FIR data object
 */
data class CaseDto(
    @SerializedName("_id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("status") val status: String,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("timeline") val timeline: List<TimelineDto>?,
    @SerializedName("assignedOfficer") val assignedOfficer: OfficerDto?
)

/**
 * TimelineDto - Case progress timeline entry
 */
data class TimelineDto(
    @SerializedName("status") val status: String,
    @SerializedName("note") val note: String,
    @SerializedName("timestamp") val timestamp: String
)

/**
 * VehicleResponse - Vehicle lookup response
 */
data class VehicleResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("vehicle") val vehicle: VehicleDto?,
    @SerializedName("message") val message: String?
)

/**
 * VehicleDto - Vehicle data from database
 */
data class VehicleDto(
    @SerializedName("_id") val id: String,
    @SerializedName("plateNumber") val plateNumber: String,
    @SerializedName("ownerName") val ownerName: String,
    @SerializedName("ownerPhone") val ownerPhone: String,
    @SerializedName("vehicleType") val vehicleType: String,
    @SerializedName("model") val model: String,
    @SerializedName("color") val color: String,
    @SerializedName("isStolen") val isStolen: Boolean,
    @SerializedName("isSuspected") val isSuspected: Boolean
)

/**
 * FaceRecognitionResponse - AI face recognition result
 */
data class FaceRecognitionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("match") val match: Boolean,
    @SerializedName("criminalId") val criminalId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("confidence") val confidence: Double?,
    @SerializedName("crimeHistory") val crimeHistory: List<String>?,
    @SerializedName("message") val message: String?
)

/**
 * AnprResponse - ANPR plate detection result
 */
data class AnprResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("plateNumber") val plateNumber: String?,
    @SerializedName("confidence") val confidence: Double?,
    @SerializedName("vehicle") val vehicle: VehicleDto?,
    @SerializedName("message") val message: String?
)

/**
 * WeaponDetectionResponse - Weapon detection from camera
 */
data class WeaponDetectionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("weaponDetected") val weaponDetected: Boolean,
    @SerializedName("detections") val detections: List<DetectionDto>?,
    @SerializedName("message") val message: String?
)

/**
 * DetectionDto - Single weapon detection bounding box
 */
data class DetectionDto(
    @SerializedName("label") val label: String,       // "gun", "knife"
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("bbox") val bbox: List<Float>     // [x1, y1, x2, y2]
)

/**
 * ComplaintClassifyResponse - NLP classification result
 */
data class ComplaintClassifyResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("category") val category: String?,  // Theft, Cybercrime, etc.
    @SerializedName("confidence") val confidence: Double?,
    @SerializedName("allScores") val allScores: Map<String, Double>?
)

/**
 * HotspotsResponse - Crime hotspot GeoJSON data
 */
data class HotspotsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("hotspots") val hotspots: List<HotspotDto>
)

/**
 * HotspotDto - Crime hotspot with risk level
 */
data class HotspotDto(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("riskScore") val riskScore: Double,  // 0.0 - 1.0
    @SerializedName("crimeCount") val crimeCount: Int,
    @SerializedName("area") val area: String
)

/**
 * ChatResponse - AI chatbot response
 */
data class ChatResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("response") val response: String,
    @SerializedName("sessionId") val sessionId: String
)

/**
 * ApiErrorDto - Generic API error response
 */
data class ApiErrorDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String,
    @SerializedName("errors") val errors: List<String>? = null
)

// =============================================================================
// PCR VAN DTOs
// =============================================================================

/**
 * PcrVanDto - PCR van from server
 */
data class PcrVanDto(
    @SerializedName("_id") val id: String,
    @SerializedName("vehicleName") val vehicleName: String,
    @SerializedName("plateNo") val plateNo: String,
    @SerializedName("model") val model: String?,
    @SerializedName("color") val color: String?,
    @SerializedName("station") val station: String?,
    @SerializedName("status") val status: String,       // Available, Busy, Off-Duty, Maintenance
    @SerializedName("assignedOfficer") val assignedOfficer: OfficerDto?,
    @SerializedName("coDriver") val coDriver: OfficerDto?,
    @SerializedName("location") val location: PcrLocationDto?,
    @SerializedName("lastSeen") val lastSeen: String?,
    @SerializedName("notes") val notes: String?
)

data class PcrLocationDto(
    @SerializedName("coordinates") val coordinates: List<Double>?,  // [lng, lat]
    @SerializedName("address") val address: String?
)

/**
 * PcrVansListResponse - Response for GET /api/pcr-vans
 */
data class PcrVansListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int,
    @SerializedName("vans") val vans: List<PcrVanDto>
)

/**
 * PcrVanResponse - Single PCR van response
 */
data class PcrVanResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("van") val van: PcrVanDto?,
    @SerializedName("message") val message: String?
)

/**
 * UpdatePcrVanRequest - Body for PATCH /api/pcr-vans/:id
 */
data class UpdatePcrVanRequest(
    @SerializedName("status") val status: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("vehicleName") val vehicleName: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("station") val station: String? = null
)

/**
 * UpdateVanLocationRequest - Body for PATCH /api/pcr-vans/:id/location
 */
data class UpdateVanLocationRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("address") val address: String? = null
)

// =============================================================================
// PROFILE DTOs
// =============================================================================

data class UpdateProfileRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("phone") val phone: String? = null
)

data class ProfileResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("user") val user: UserDto?
)

// =============================================================================
// SOS HISTORY / ACTIVE SOS DTOs
// =============================================================================

data class SosUserDto(
    @SerializedName("_id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String
)

data class SosLocationDto(
    @SerializedName("coordinates") val coordinates: List<Double>?, // [lng, lat]
    @SerializedName("address") val address: String?
)

data class SosLogDto(
    @SerializedName("_id") val id: String,
    @SerializedName("triggeredBy") val triggeredBy: SosUserDto?,
    @SerializedName("location") val location: SosLocationDto?,
    @SerializedName("type") val type: String,
    @SerializedName("status") val status: String,
    @SerializedName("respondingOfficer") val respondingOfficer: OfficerDto?,
    @SerializedName("responseTimeSeconds") val responseTimeSeconds: Int?,
    @SerializedName("resolvedAt") val resolvedAt: String?,
    @SerializedName("createdAt") val createdAt: String
)

data class SosHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int,
    @SerializedName("logs") val logs: List<SosLogDto>
)

data class AssignSosOfficerRequest(
    @SerializedName("officerId") val officerId: String
)

data class AssignSosResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("sosId") val sosId: String?,
    @SerializedName("officerId") val officerId: String?
)

// =============================================================================
// CRIMINAL DTOs
// =============================================================================

data class CrimeHistoryDto(
    @SerializedName("offense") val offense: String,
    @SerializedName("date") val date: String?,
    @SerializedName("status") val status: String?
)

data class CriminalDto(
    @SerializedName("_id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("age") val age: Int?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("dangerLevel") val dangerLevel: String?,
    @SerializedName("lastKnownAddress") val lastKnownAddress: String?,
    @SerializedName("photo") val photo: String?,
    @SerializedName("warrantStatus") val warrantStatus: Boolean,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("crimeHistory") val crimeHistory: List<CrimeHistoryDto>?,
    @SerializedName("hasEmbedding") val hasEmbedding: Boolean,
    @SerializedName("description") val description: String?
)

data class CriminalListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("criminals") val criminals: List<CriminalDto>,
    @SerializedName("total") val total: Int
)

data class CriminalDetailResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("criminal") val criminal: CriminalDto?
)
