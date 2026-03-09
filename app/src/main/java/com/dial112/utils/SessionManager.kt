package com.dial112.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

// DataStore extension property for Context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dial112_session")

/**
 * SessionManager - Manages JWT token storage using DataStore
 *
 * DataStore is the modern, coroutine-friendly replacement for SharedPreferences.
 * Token is stored securely (obfuscated in release builds with ProGuard).
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
    }

    /** Save JWT token after login/register */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    /** Get current JWT token (returns null if not logged in) */
    suspend fun getToken(): String? {
        return context.dataStore.data.map { prefs ->
            prefs[TOKEN_KEY]
        }.first()
    }

    /** Save user role for quick role-based navigation */
    suspend fun saveUserRole(role: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ROLE_KEY] = role
        }
    }

    /** Get cached user role */
    suspend fun getUserRole(): String? {
        return context.dataStore.data.map { prefs ->
            prefs[USER_ROLE_KEY]
        }.first()
    }

    /** Clear all session data on logout */
    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

// =============================================================================
// AuthInterceptor - OkHttp Interceptor that attaches JWT to every request
// =============================================================================

/**
 * AuthInterceptor - Automatically attaches "Authorization: Bearer <token>"
 * header to every outgoing Retrofit request.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Get token synchronously (interceptors are called on IO thread)
        val token = runBlocking { sessionManager.getToken() }

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}
