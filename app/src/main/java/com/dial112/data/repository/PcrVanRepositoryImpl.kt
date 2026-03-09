package com.dial112.data.repository

import com.dial112.data.remote.api.PcrVanApiService
import com.dial112.data.remote.dto.UpdatePcrVanRequest
import com.dial112.data.remote.dto.UpdateVanLocationRequest
import com.dial112.domain.model.*
import com.dial112.domain.repository.PcrVanRepository
import javax.inject.Inject

class PcrVanRepositoryImpl @Inject constructor(
    private val apiService: PcrVanApiService
) : PcrVanRepository {

    override suspend fun getAllVans(): Resource<List<PcrVan>> {
        return try {
            val response = apiService.getAllVans()
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()!!.vans.map { it.toDomain() })
            } else {
                Resource.Error(response.body()?.vans.let { "Failed to load PCR vans" })
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun getMyVan(): Resource<PcrVan?> {
        return try {
            val response = apiService.getMyVan()
            if (response.isSuccessful) {
                Resource.Success(response.body()?.van?.toDomain())
            } else {
                Resource.Error("Failed to load your PCR van")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun updateVan(vanId: String, status: String?, notes: String?): Resource<PcrVan> {
        return try {
            val response = apiService.updateVan(vanId, UpdatePcrVanRequest(status = status, notes = notes))
            if (response.isSuccessful && response.body()?.success == true) {
                val van = response.body()!!.van ?: return Resource.Error("No van data returned")
                Resource.Success(van.toDomain())
            } else {
                Resource.Error(response.body()?.message ?: "Update failed")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun updateVanLocation(vanId: String, latitude: Double, longitude: Double): Resource<Unit> {
        return try {
            val response = apiService.updateVanLocation(vanId, UpdateVanLocationRequest(latitude, longitude))
            if (response.isSuccessful) Resource.Success(Unit)
            else Resource.Error("Location update failed")
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────
    private fun com.dial112.data.remote.dto.PcrVanDto.toDomain(): PcrVan {
        val lat = location?.coordinates?.getOrNull(1) ?: 0.0
        val lng = location?.coordinates?.getOrNull(0) ?: 0.0
        return PcrVan(
            id = id,
            vehicleName = vehicleName,
            plateNo = plateNo,
            model = model ?: "",
            color = color ?: "White",
            station = station ?: "",
            status = status,
            assignedOfficer = assignedOfficer?.let {
                Officer(
                    id = it.id,
                    name = it.name,
                    badgeId = it.badgeId,
                    phone = it.phone,
                    profileImage = it.profileImage,
                    station = it.station ?: ""
                )
            },
            coDriver = coDriver?.let {
                Officer(
                    id = it.id,
                    name = it.name,
                    badgeId = it.badgeId,
                    phone = it.phone,
                    profileImage = it.profileImage,
                    station = it.station ?: ""
                )
            },
            latitude = lat,
            longitude = lng,
            address = location?.address ?: "",
            lastSeen = lastSeen,
            notes = notes ?: ""
        )
    }
}
