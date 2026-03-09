package com.dial112.domain.repository

import com.dial112.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository Interfaces - Domain layer contracts
 * All implementations are in the data layer.
 * This ensures the domain layer has no dependency on data sources.
 */

/**
 * AuthRepository - Contract for authentication operations
 */
interface AuthRepository {
    /** Login with email/password - returns JWT token and user */
    suspend fun login(email: String, password: String): Resource<User>

    /** Register a new account */
    suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String,
        role: String,
        badgeId: String? = null
    ): Resource<User>

    /** Send password-reset OTP to the given email */
    suspend fun forgotPassword(email: String): Resource<String>

    /** Verify OTP and set a new password */
    suspend fun resetPassword(email: String, otp: String, newPassword: String): Resource<String>

    /** Get the currently cached logged-in user */
    fun observeCurrentUser(): Flow<User?>

    /** Logout and clear local session */
    suspend fun logout()

    /** Check if user is already logged in (has cached JWT) */
    suspend fun isLoggedIn(): Boolean
}

/**
 * SosRepository - Contract for SOS emergency operations
 */
interface SosRepository {
    /** Trigger an SOS alert with current GPS coordinates */
    suspend fun triggerSos(
        latitude: Double,
        longitude: Double,
        address: String,
        type: String = "SOS"
    ): Resource<SosEmergency>

    /** Get SOS history from local cache */
    fun observeSosHistory(): Flow<List<SosEmergency>>
}

/**
 * CasesRepository - Contract for FIR / case management
 */
interface CasesRepository {
    /** File a new FIR with optional image */
    suspend fun fileCase(
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        imagePath: String?
    ): Resource<Case>

    /** Get a single case by ID */
    suspend fun getCaseById(caseId: String): Resource<Case>

    /** Update a case status (Police only) */
    suspend fun updateCase(caseId: String, status: String, notes: String?): Resource<Case>

    /** Observe all cases from local cache (with server sync) */
    fun observeCases(): Flow<List<Case>>

    /** Force refresh cases from server */
    suspend fun refreshCases(): Resource<List<Case>>
}

/**
 * VehicleRepository - Contract for vehicle lookup
 */
interface VehicleRepository {
    /** Lookup vehicle by number plate */
    suspend fun getVehicleByPlate(plateNumber: String): Resource<Vehicle>
}

/**
 * AiRepository - Contract for all AI microservice operations
 */
interface AiRepository {
    /** Recognize a face from image bytes */
    suspend fun recognizeFace(imageBytes: ByteArray): Resource<FaceMatch>

    /** Detect vehicle number plate */
    suspend fun detectNumberPlate(imageBytes: ByteArray): Resource<String>

    /** Detect weapons in an image */
    suspend fun detectWeapon(imageBytes: ByteArray): Resource<WeaponDetection>

    /** Classify a complaint text using NLP */
    suspend fun classifyComplaint(text: String): Resource<ComplaintCategory>

    /** Get crime hotspot clusters */
    suspend fun getCrimeHotspots(): Resource<List<Hotspot>>

    /** Chat with AI assistant */
    suspend fun chat(message: String, sessionId: String): Resource<String>
}
/**
 * PcrVanRepository - Contract for PCR van operations
 */
interface PcrVanRepository {
    /** Get all PCR vans */
    suspend fun getAllVans(): Resource<List<com.dial112.domain.model.PcrVan>>

    /** Get the van assigned to the current officer */
    suspend fun getMyVan(): Resource<com.dial112.domain.model.PcrVan?>

    /** Update van details (status, notes, etc.) */
    suspend fun updateVan(vanId: String, status: String?, notes: String?): Resource<com.dial112.domain.model.PcrVan>

    /** Update van GPS location */
    suspend fun updateVanLocation(vanId: String, latitude: Double, longitude: Double): Resource<Unit>
}

/**
 * ProfileRepository - Contract for user profile operations
 */
interface ProfileRepository {
    suspend fun getProfile(): Resource<com.dial112.domain.model.User>
    suspend fun updateProfile(name: String?, phone: String?): Resource<com.dial112.domain.model.User>
}

/**
 * SosManagementRepository - Server-side SOS history and dispatch
 */
interface SosManagementRepository {
    suspend fun getSosHistory(): Resource<List<com.dial112.domain.model.SosLog>>
    suspend fun getActiveSos(): Resource<List<com.dial112.domain.model.SosLog>>
    suspend fun assignOfficer(sosId: String, officerId: String): Resource<String>
}

/**
 * CriminalRepository - Contract for criminal database search
 */
interface CriminalRepository {
    suspend fun searchCriminals(query: String?, page: Int): Resource<List<com.dial112.domain.model.Criminal>>
    suspend fun getCriminalById(id: String): Resource<com.dial112.domain.model.Criminal>
}