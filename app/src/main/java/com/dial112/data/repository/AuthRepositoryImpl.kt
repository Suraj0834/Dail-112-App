package com.dial112.data.repository

import android.content.Context
import com.dial112.data.local.dao.UserDao
import com.dial112.data.local.entity.UserEntity
import com.dial112.data.remote.api.AuthApiService
import com.dial112.data.remote.dto.ForgotPasswordRequest
import com.dial112.data.remote.dto.LoginRequest
import com.dial112.data.remote.dto.RegisterRequest
import com.dial112.data.remote.dto.ResetPasswordRequest
import com.dial112.domain.model.*
import com.dial112.domain.repository.AuthRepository
import com.dial112.utils.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * AuthRepositoryImpl - Concrete implementation of AuthRepository
 *
 * Uses:
 * - Remote: AuthApiService (Retrofit) for API calls
 * - Local: UserDao (Room) for session caching
 * - SessionManager: DataStore for JWT token storage
 */
class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : AuthRepository {

    override suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val response = apiService.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                val token = body.token ?: return Resource.Error("No token received")
                val userDto = body.user ?: return Resource.Error("No user data received")

                // Save JWT token securely
                sessionManager.saveToken(token)

                // Cache user locally
                val userEntity = UserEntity(
                    id = userDto.id,
                    name = userDto.name,
                    email = userDto.email,
                    phone = userDto.phone,
                    role = userDto.role,
                    badgeId = userDto.badgeId,
                    profileImage = userDto.profileImage,
                    station = userDto.station,
                    jwtToken = token
                )
                userDao.insertUser(userEntity)

                Resource.Success(userEntity.toDomainModel())
            } else {
                Resource.Error(response.body()?.message ?: "Login failed")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String,
        role: String,
        badgeId: String?
    ): Resource<User> {
        return try {
            val response = apiService.register(
                RegisterRequest(name, email, password, phone, role, badgeId)
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                val token = body.token ?: return Resource.Error("No token received")
                val userDto = body.user ?: return Resource.Error("No user data received")

                sessionManager.saveToken(token)

                val userEntity = UserEntity(
                    id = userDto.id,
                    name = userDto.name,
                    email = userDto.email,
                    phone = userDto.phone,
                    role = userDto.role,
                    badgeId = userDto.badgeId,
                    profileImage = userDto.profileImage,
                    station = userDto.station,
                    jwtToken = token
                )
                userDao.insertUser(userEntity)

                Resource.Success(userEntity.toDomainModel())
            } else {
                Resource.Error(response.body()?.message ?: "Registration failed")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun forgotPassword(email: String): Resource<String> {
        return try {
            val response = apiService.forgotPassword(ForgotPasswordRequest(email))
            if (response.isSuccessful && response.body()?.success == true)
                Resource.Success(response.body()!!.message)
            else
                Resource.Error(response.body()?.message ?: "Failed to send OTP")
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    override suspend fun resetPassword(email: String, otp: String, newPassword: String): Resource<String> {
        return try {
            val response = apiService.resetPassword(ResetPasswordRequest(email, otp, newPassword))
            if (response.isSuccessful && response.body()?.success == true)
                Resource.Success(response.body()!!.message)
            else
                Resource.Error(response.body()?.message ?: "Password reset failed")
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}")
        }
    }

    override fun observeCurrentUser(): Flow<User?> {        return userDao.observeCurrentUser().map { it?.toDomainModel() }
    }

    override suspend fun logout() {
        sessionManager.clearSession()
        userDao.clearUsers()
    }

    override suspend fun isLoggedIn(): Boolean {
        return sessionManager.getToken() != null
    }

    // Extension function: UserEntity -> Domain User
    private fun UserEntity.toDomainModel() = User(
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
