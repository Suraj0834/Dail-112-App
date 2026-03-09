package com.dial112.data.remote.api

import com.dial112.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * AuthApiService - Retrofit interface for authentication endpoints
 * Handles login and registration for Citizens and Police officers
 */
interface AuthApiService {

    /**
     * Register a new user (Citizen or Police)
     * POST /api/auth/register
     */
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    /**
     * Login with credentials
     * POST /api/auth/login
     */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    /**
     * Send OTP to email for password reset
     * POST /api/auth/forgot-password
     */
    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    /**
     * Verify OTP and set new password
     * POST /api/auth/reset-password
     */
    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<MessageResponse>
}

/**
 * SosApiService - Retrofit interface for SOS emergency endpoints
 */
interface SosApiService {

    /**
     * Trigger SOS emergency alert with current location
     * POST /api/sos
     */
    @POST("api/sos")
    suspend fun triggerSos(@Body request: SosRequest): Response<SosResponse>
}

/**
 * CasesApiService - Retrofit interface for FIR / Case management
 */
interface CasesApiService {

    /**
     * File a new FIR / complaint
     * POST /api/cases
     */
    @Multipart
    @POST("api/cases")
    suspend fun createCase(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<CaseResponse>

    /**
     * Get a single case by ID (for timeline tracking)
     * GET /api/cases/:id
     */
    @GET("api/cases/{id}")
    suspend fun getCaseById(@Path("id") caseId: String): Response<CaseResponse>

    /**
     * Update a case status (Police officers only)
     * PUT /api/cases/:id
     */
    @PUT("api/cases/{id}")
    suspend fun updateCase(
        @Path("id") caseId: String,
        @Body request: UpdateCaseRequest
    ): Response<CaseResponse>

    /**
     * Get all cases for current user
     * GET /api/cases
     */
    @GET("api/cases")
    suspend fun getUserCases(): Response<CasesListResponse>
}

/**
 * VehicleApiService - Retrofit interface for ANPR vehicle lookup
 */
interface VehicleApiService {

    /**
     * Lookup a vehicle by number plate
     * GET /api/vehicles/:number
     */
    @GET("api/vehicles/{number}")
    suspend fun getVehicleByNumber(
        @Path("number") plateNumber: String
    ): Response<VehicleResponse>
}

/**
 * AiApiService - Retrofit interface for all AI microservice proxied endpoints
 */
interface AiApiService {

    /**
     * Face recognition from image
     * POST /api/ai/face-recognition
     */
    @Multipart
    @POST("api/ai/face-recognition")
    suspend fun recognizeFace(
        @Part image: MultipartBody.Part
    ): Response<FaceRecognitionResponse>

    /**
     * ANPR - Automatic Number Plate Recognition
     * POST /api/ai/anpr
     */
    @Multipart
    @POST("api/ai/anpr")
    suspend fun detectNumberPlate(
        @Part image: MultipartBody.Part
    ): Response<AnprResponse>

    /**
     * Weapon detection from camera image
     * POST /api/ai/detect-weapon
     */
    @Multipart
    @POST("api/ai/detect-weapon")
    suspend fun detectWeapon(
        @Part image: MultipartBody.Part
    ): Response<WeaponDetectionResponse>

    /**
     * NLP complaint classification
     * POST /api/ai/classify-complaint
     */
    @POST("api/ai/classify-complaint")
    suspend fun classifyComplaint(
        @Body request: ComplaintClassifyRequest
    ): Response<ComplaintClassifyResponse>

    /**
     * Get crime hotspots (GeoJSON clusters)
     * GET /api/ai/hotspots
     */
    @GET("api/ai/hotspots")
    suspend fun getCrimeHotspots(): Response<HotspotsResponse>

    /**
     * AI Chatbot message
     * POST /api/ai/chat
     */
    @POST("api/ai/chat")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>
}

/**
 * PcrVanApiService - Retrofit interface for PCR van management
 */
interface PcrVanApiService {

    /** GET /api/pcr-vans — list all PCR vans */
    @GET("api/pcr-vans")
    suspend fun getAllVans(): Response<PcrVansListResponse>

    /** GET /api/pcr-vans/mine — get PCR van assigned to me */
    @GET("api/pcr-vans/mine")
    suspend fun getMyVan(): Response<PcrVanResponse>

    /** PATCH /api/pcr-vans/:id — update van details */
    @PATCH("api/pcr-vans/{id}")
    suspend fun updateVan(
        @Path("id") vanId: String,
        @Body request: UpdatePcrVanRequest
    ): Response<PcrVanResponse>

    /** PATCH /api/pcr-vans/:id/location — update van GPS */
    @PATCH("api/pcr-vans/{id}/location")
    suspend fun updateVanLocation(
        @Path("id") vanId: String,
        @Body request: UpdateVanLocationRequest
    ): Response<MessageResponse>
}
/**
 * ProfileApiService - Profile management endpoints
 */
interface ProfileApiService {

    @GET("api/auth/profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ProfileResponse>
}

/**
 * SosManagementApiService - Extended SOS endpoints for history and police dispatch
 */
interface SosManagementApiService {

    @GET("api/sos/history")
    suspend fun getSosHistory(): Response<SosHistoryResponse>

    @GET("api/sos/active")
    suspend fun getActiveSos(): Response<SosHistoryResponse>

    @POST("api/sos/{sosId}/assign")
    suspend fun assignOfficer(
        @Path("sosId") sosId: String,
        @Body request: AssignSosOfficerRequest
    ): Response<AssignSosResponse>
}

/**
 * CriminalApiService - Criminal database lookup (Police only)
 */
interface CriminalApiService {

    @GET("api/criminals")
    suspend fun searchCriminals(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 15
    ): Response<CriminalListResponse>

    @GET("api/criminals/{id}")
    suspend fun getCriminalById(@Path("id") id: String): Response<CriminalDetailResponse>
}