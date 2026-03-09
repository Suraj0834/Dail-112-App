package com.dial112.data.repository

import com.dial112.data.local.dao.SosLogDao
import com.dial112.data.local.entity.SosLogEntity
import com.dial112.data.remote.api.SosApiService
import com.dial112.data.remote.dto.SosRequest
import com.dial112.domain.model.*
import com.dial112.domain.repository.SosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject

/**
 * SosRepositoryImpl - SOS Emergency repository implementation
 */
class SosRepositoryImpl @Inject constructor(
    private val apiService: SosApiService,
    private val sosLogDao: SosLogDao
) : SosRepository {

    override suspend fun triggerSos(
        latitude: Double,
        longitude: Double,
        address: String,
        type: String
    ): Resource<SosEmergency> {
        // Immediately save to local log (before API call, in case network fails)
        val localId = UUID.randomUUID().toString()
        val localLog = SosLogEntity(
            id = localId,
            latitude = latitude,
            longitude = longitude,
            address = address,
            type = type,
            synced = false
        )
        sosLogDao.insertSosLog(localLog)

        return try {
            val response = apiService.triggerSos(
                SosRequest(latitude, longitude, address, type)
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                // Mark as synced
                sosLogDao.markAsSynced(localId)

                val nearestOfficer = body.nearestOfficer?.let { o ->
                    Officer(o.id, o.name, o.badgeId, o.phone, o.profileImage, o.station ?: "", o.distance)
                }

                Resource.Success(
                    SosEmergency(
                        id = body.sosId ?: localId,
                        latitude = latitude,
                        longitude = longitude,
                        address = address,
                        type = type,
                        nearestOfficer = nearestOfficer
                    )
                )
            } else {
                Resource.Error(response.body()?.message ?: "SOS dispatch failed")
            }
        } catch (e: Exception) {
            // Offline: SOS was saved locally, will sync when connection restores
            Resource.Error("SOS saved locally. Will sync when online. Error: ${e.localizedMessage}")
        }
    }

    override fun observeSosHistory(): Flow<List<SosEmergency>> {
        return sosLogDao.observeAllSosLogs().map { entities ->
            entities.map { log ->
                SosEmergency(log.id, log.latitude, log.longitude, log.address, log.type, null)
            }
        }
    }
}


/**
 * VehicleRepositoryImpl - Vehicle lookup repository
 */
class VehicleRepositoryImpl @Inject constructor(
    private val apiService: com.dial112.data.remote.api.VehicleApiService
) : com.dial112.domain.repository.VehicleRepository {

    override suspend fun getVehicleByPlate(plateNumber: String): Resource<Vehicle> {
        return try {
            val response = apiService.getVehicleByNumber(plateNumber.uppercase().trim())
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()!!.vehicle!!
                Resource.Success(
                    Vehicle(
                        id = dto.id,
                        plateNumber = dto.plateNumber,
                        ownerName = dto.ownerName,
                        ownerPhone = dto.ownerPhone,
                        vehicleType = dto.vehicleType,
                        model = dto.model,
                        color = dto.color,
                        isStolen = dto.isStolen,
                        isSuspected = dto.isSuspected
                    )
                )
            } else {
                Resource.Error(response.body()?.message ?: "Vehicle not found")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }
}


/**
 * AiRepositoryImpl - AI microservice repository
 */
class AiRepositoryImpl @Inject constructor(
    private val apiService: com.dial112.data.remote.api.AiApiService
) : com.dial112.domain.repository.AiRepository {

    override suspend fun recognizeFace(imageBytes: ByteArray): Resource<FaceMatch> {
        return try {
            val part = createImagePart(imageBytes, "face_image.jpg")
            val response = apiService.recognizeFace(part)
            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                Resource.Success(
                    FaceMatch(body.match, body.criminalId, body.name, body.confidence, body.crimeHistory)
                )
            } else Resource.Error(response.body()?.message ?: "Face recognition failed")
        } catch (e: Exception) { Resource.Error("Error: ${e.localizedMessage}") }
    }

    override suspend fun detectNumberPlate(imageBytes: ByteArray): Resource<String> {
        return try {
            val part = createImagePart(imageBytes, "plate_image.jpg")
            val response = apiService.detectNumberPlate(part)
            if (response.isSuccessful && response.body()?.success == true) {
                val plate = response.body()?.plateNumber
                if (plate != null) Resource.Success(plate)
                else Resource.Error("No plate detected")
            } else Resource.Error("ANPR failed")
        } catch (e: Exception) { Resource.Error("Error: ${e.localizedMessage}") }
    }

    override suspend fun detectWeapon(imageBytes: ByteArray): Resource<WeaponDetection> {
        return try {
            val part = createImagePart(imageBytes, "weapon_image.jpg")
            val response = apiService.detectWeapon(part)
            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                val detections = body.detections?.map {
                    Detection(it.label, it.confidence, it.bbox)
                } ?: emptyList()
                Resource.Success(WeaponDetection(body.weaponDetected, detections))
            } else Resource.Error("Weapon detection failed")
        } catch (e: Exception) { Resource.Error("Error: ${e.localizedMessage}") }
    }

    override suspend fun classifyComplaint(text: String): Resource<ComplaintCategory> {
        return try {
            val response = apiService.classifyComplaint(
                com.dial112.data.remote.dto.ComplaintClassifyRequest(text)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                Resource.Success(
                    ComplaintCategory(
                        body.category ?: "Other",
                        body.confidence ?: 0.0,
                        body.allScores ?: emptyMap()
                    )
                )
            } else Resource.Error("Classification failed")
        } catch (e: Exception) { Resource.Error("Error: ${e.localizedMessage}") }
    }

    override suspend fun getCrimeHotspots(): Resource<List<Hotspot>> {
        return try {
            val response = apiService.getCrimeHotspots()
            if (response.isSuccessful && response.body()?.success == true) {
                val hotspots = response.body()!!.hotspots.map {
                    Hotspot(it.latitude, it.longitude, it.riskScore, it.crimeCount, it.area)
                }
                Resource.Success(hotspots)
            } else Resource.Error("Failed to load hotspots")
        } catch (e: Exception) { Resource.Error("Error: ${e.localizedMessage}") }
    }

    override suspend fun chat(message: String, sessionId: String): Resource<String> {
        return try {
            val response = apiService.chat(com.dial112.data.remote.dto.ChatRequest(message, sessionId))
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()!!.response)
            } else Resource.Error("Chat failed")
        } catch (e: Exception) { Resource.Error("Error: ${e.localizedMessage}") }
    }

    private fun createImagePart(bytes: ByteArray, filename: String): okhttp3.MultipartBody.Part {
        val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        return okhttp3.MultipartBody.Part.createFormData("image", filename, body)
    }
}
