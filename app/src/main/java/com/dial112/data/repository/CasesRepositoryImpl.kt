package com.dial112.data.repository

import com.dial112.data.local.dao.CaseDao
import com.dial112.data.local.entity.CaseEntity
import com.dial112.data.remote.api.CasesApiService
import com.dial112.domain.model.*
import com.dial112.domain.repository.CasesRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

/**
 * CasesRepositoryImpl - FIR / Case management repository
 *
 * Handles:
 * - Filing new cases with optional image upload (multipart)
 * - Case retrieval with offline caching
 * - Case status updates (Police only)
 */
class CasesRepositoryImpl @Inject constructor(
    private val apiService: CasesApiService,
    private val caseDao: CaseDao
) : CasesRepository {

    private val gson = Gson()

    override suspend fun fileCase(
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        imagePath: String?
    ): Resource<Case> {
        return try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
            val latBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val lonBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            // Optional image part
            val imagePart = imagePath?.let { path ->
                val file = File(path)
                val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", file.name, requestBody)
            }

            val response = apiService.createCase(
                titleBody, descBody, categoryBody, latBody, lonBody, imagePart
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val caseDto = response.body()!!.case!!
                val entity = caseDto.toEntity(gson)
                caseDao.insertCase(entity)
                Resource.Success(entity.toDomainModel(gson))
            } else {
                Resource.Error(response.body()?.message ?: "Failed to file case")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun getCaseById(caseId: String): Resource<Case> {
        return try {
            val response = apiService.getCaseById(caseId)
            if (response.isSuccessful && response.body()?.success == true) {
                val caseDto = response.body()!!.case!!
                val entity = caseDto.toEntity(gson)
                caseDao.insertCase(entity)
                Resource.Success(entity.toDomainModel(gson))
            } else {
                // Fallback to local cache
                val cached = caseDao.getCaseById(caseId)
                if (cached != null) Resource.Success(cached.toDomainModel(gson))
                else Resource.Error("Case not found")
            }
        } catch (e: Exception) {
            // Offline fallback
            val cached = caseDao.getCaseById(caseId)
            if (cached != null) Resource.Success(cached.toDomainModel(gson))
            else Resource.Error("Offline - case not available")
        }
    }

    override suspend fun updateCase(caseId: String, status: String, notes: String?): Resource<Case> {
        return try {
            val response = apiService.updateCase(
                caseId,
                com.dial112.data.remote.dto.UpdateCaseRequest(status, notes)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val caseDto = response.body()!!.case!!
                val entity = caseDto.toEntity(gson)
                caseDao.insertCase(entity)
                Resource.Success(entity.toDomainModel(gson))
            } else {
                Resource.Error(response.body()?.message ?: "Update failed")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    override fun observeCases(): Flow<List<Case>> {
        return caseDao.observeAllCases().map { entities ->
            entities.map { it.toDomainModel(gson) }
        }
    }

    override suspend fun refreshCases(): Resource<List<Case>> {
        return try {
            val response = apiService.getUserCases()
            if (response.isSuccessful && response.body()?.success == true) {
                val cases = response.body()!!.cases
                val entities = cases.map { it.toEntity(gson) }
                caseDao.insertCases(entities)
                Resource.Success(entities.map { it.toDomainModel(gson) })
            } else {
                Resource.Error("Failed to refresh cases")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    // Extension: Convert DTO to Entity
    private fun com.dial112.data.remote.dto.CaseDto.toEntity(gson: Gson) = CaseEntity(
        id = id,
        title = title,
        description = description,
        category = category,
        status = status,
        imageUrl = imageUrl,
        latitude = latitude,
        longitude = longitude,
        createdAt = createdAt,
        updatedAt = updatedAt,
        timelineJson = gson.toJson(timeline ?: emptyList<Any>()),
        assignedOfficerJson = assignedOfficer?.let { gson.toJson(it) }
    )

    // Extension: Convert Entity to Domain Model
    private fun CaseEntity.toDomainModel(gson: Gson): Case {
        val timelineType = object : com.google.gson.reflect.TypeToken<List<TimelineEntry>>() {}.type
        val timeline: List<TimelineEntry> = try {
            gson.fromJson(timelineJson, timelineType) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        val officerDto: com.dial112.data.remote.dto.OfficerDto? = try {
            assignedOfficerJson?.let {
                gson.fromJson(it, com.dial112.data.remote.dto.OfficerDto::class.java)
            }
        } catch (e: Exception) { null }

        return Case(
            id = id,
            title = title,
            description = description,
            category = CaseCategory.entries.find { it.name.equals(category, ignoreCase = true) }
                ?: CaseCategory.OTHER,
            status = CaseStatus.entries.find { it.name.equals(status, ignoreCase = true) }
                ?: CaseStatus.PENDING,
            imageUrl = imageUrl,
            latitude = latitude,
            longitude = longitude,
            createdAt = createdAt,
            updatedAt = updatedAt,
            timeline = timeline,
            assignedOfficer = officerDto?.let {
                Officer(it.id, it.name, it.badgeId, it.phone, it.profileImage, it.station)
            }
        )
    }
}
