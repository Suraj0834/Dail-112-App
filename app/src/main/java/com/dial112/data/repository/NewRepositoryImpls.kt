package com.dial112.data.repository

import com.dial112.data.remote.api.CriminalApiService
import com.dial112.data.remote.api.ProfileApiService
import com.dial112.data.remote.api.SosManagementApiService
import com.dial112.data.remote.dto.*
import com.dial112.domain.model.*
import com.dial112.domain.repository.CriminalRepository
import com.dial112.domain.repository.ProfileRepository
import com.dial112.domain.repository.SosManagementRepository
import javax.inject.Inject

// =============================================================================
// ProfileRepositoryImpl
// =============================================================================
class ProfileRepositoryImpl @Inject constructor(
    private val api: ProfileApiService
) : ProfileRepository {

    override suspend fun getProfile(): Resource<User> = try {
        val r = api.getProfile()
        if (r.isSuccessful && r.body()?.success == true) {
            val u = r.body()!!.user!!
            Resource.Success(u.toDomain())
        } else Resource.Error(r.body()?.user?.name ?: "Failed to load profile")
    } catch (e: Exception) {
        Resource.Error("Network error: ${e.localizedMessage}")
    }

    override suspend fun updateProfile(name: String?, phone: String?): Resource<User> = try {
        val r = api.updateProfile(UpdateProfileRequest(name, phone))
        if (r.isSuccessful && r.body()?.success == true)
            Resource.Success(r.body()!!.user!!.toDomain())
        else Resource.Error(r.body()?.user?.name ?: "Update failed")
    } catch (e: Exception) {
        Resource.Error("Network error: ${e.localizedMessage}")
    }

    private fun UserDto.toDomain() = User(
        id = id,
        name = name,
        email = email,
        phone = phone,
        role = UserRole.fromString(role),
        badgeId = badgeId,
        profileImage = profileImage,
        station = station
    )
}

// =============================================================================
// SosManagementRepositoryImpl
// =============================================================================
class SosManagementRepositoryImpl @Inject constructor(
    private val api: SosManagementApiService
) : SosManagementRepository {

    override suspend fun getSosHistory(): Resource<List<SosLog>> = try {
        val r = api.getSosHistory()
        if (r.isSuccessful && r.body()?.success == true)
            Resource.Success(r.body()!!.logs.map { it.toDomain() })
        else Resource.Error("Failed to fetch SOS history")
    } catch (e: Exception) {
        Resource.Error("Network error: ${e.localizedMessage}")
    }

    override suspend fun getActiveSos(): Resource<List<SosLog>> = try {
        val r = api.getActiveSos()
        if (r.isSuccessful && r.body()?.success == true)
            Resource.Success(r.body()!!.logs.map { it.toDomain() })
        else Resource.Error("Failed to fetch active SOS alerts")
    } catch (e: Exception) {
        Resource.Error("Network error: ${e.localizedMessage}")
    }

    override suspend fun assignOfficer(sosId: String, officerId: String): Resource<String> = try {
        val r = api.assignOfficer(sosId, AssignSosOfficerRequest(officerId))
        if (r.isSuccessful && r.body()?.success == true)
            Resource.Success(r.body()!!.message ?: "Assigned successfully")
        else Resource.Error(r.body()?.message ?: "Assignment failed")
    } catch (e: Exception) {
        Resource.Error("Network error: ${e.localizedMessage}")
    }

    private fun SosLogDto.toDomain(): SosLog {
        val lat = location?.coordinates?.getOrNull(1) ?: 0.0
        val lng = location?.coordinates?.getOrNull(0) ?: 0.0
        return SosLog(
            id = id,
            latitude = lat,
            longitude = lng,
            address = location?.address ?: "",
            type = type,
            status = status,
            triggeredBy = triggeredBy?.let { SosUser(it.id, it.name, it.phone) },
            respondingOfficer = respondingOfficer?.let {
                Officer(it.id, it.name, it.badgeId, it.phone, it.profileImage, it.station ?: "", it.distance)
            },
            responseTimeSeconds = responseTimeSeconds,
            resolvedAt = resolvedAt,
            createdAt = createdAt
        )
    }
}

// =============================================================================
// CriminalRepositoryImpl
// =============================================================================
class CriminalRepositoryImpl @Inject constructor(
    private val api: CriminalApiService
) : CriminalRepository {

    override suspend fun searchCriminals(query: String?, page: Int): Resource<List<Criminal>> = try {
        val r = api.searchCriminals(query, page)
        if (r.isSuccessful && r.body()?.success == true)
            Resource.Success(r.body()!!.criminals.map { it.toDomain() })
        else Resource.Error("Failed to search criminals")
    } catch (e: Exception) {
        Resource.Error("Network error: ${e.localizedMessage}")
    }

    override suspend fun getCriminalById(id: String): Resource<Criminal> = try {
        val r = api.getCriminalById(id)
        if (r.isSuccessful && r.body()?.success == true && r.body()!!.criminal != null)
            Resource.Success(r.body()!!.criminal!!.toDomain())
        else Resource.Error("Criminal record not found")
    } catch (e: Exception) {
        Resource.Error("Network error: ${e.localizedMessage}")
    }

    private fun CriminalDto.toDomain() = Criminal(
        id = id,
        name = name,
        age = age,
        gender = gender,
        dangerLevel = dangerLevel ?: "MEDIUM",
        lastKnownAddress = lastKnownAddress,
        photo = photo,
        warrantStatus = warrantStatus,
        isActive = isActive,
        crimeHistory = crimeHistory?.map { CrimeHistoryEntry(it.offense, it.date, it.status) } ?: emptyList(),
        hasEmbedding = hasEmbedding,
        description = description
    )
}
