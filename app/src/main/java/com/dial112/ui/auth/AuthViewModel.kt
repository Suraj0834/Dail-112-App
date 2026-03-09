package com.dial112.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dial112.domain.model.Resource
import com.dial112.domain.model.User
import com.dial112.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AuthViewModel - Manages authentication state for Login and Register screens
 *
 * Uses:
 * - AuthRepository for login/register API calls
 * - LiveData to expose state to Fragments (observe-based UI updates)
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // -------------------------------------------------------------------------
    // Login state
    // -------------------------------------------------------------------------
    private val _loginState = MutableLiveData<Resource<User>>()
    val loginState: LiveData<Resource<User>> = _loginState

    // -------------------------------------------------------------------------
    // Register state
    // -------------------------------------------------------------------------
    private val _registerState = MutableLiveData<Resource<User>>()
    val registerState: LiveData<Resource<User>> = _registerState

    // -------------------------------------------------------------------------
    // Forgot password / Reset password state
    // -------------------------------------------------------------------------
    private val _forgotPasswordState = MutableLiveData<Resource<String>>()
    val forgotPasswordState: LiveData<Resource<String>> = _forgotPasswordState

    private val _resetPasswordState = MutableLiveData<Resource<String>>()
    val resetPasswordState: LiveData<Resource<String>> = _resetPasswordState

    // -------------------------------------------------------------------------
    // Session check state
    // -------------------------------------------------------------------------
    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    /**
     * Login user with email and password
     */
    fun login(email: String, password: String) {
        if (!validateLoginInput(email, password)) return

        viewModelScope.launch {
            _loginState.value = Resource.Loading
            _loginState.value = authRepository.login(email, password)
        }
    }

    /**
     * Register a new user account
     */
    fun register(
        name: String,
        email: String,
        password: String,
        phone: String,
        role: String,
        badgeId: String? = null
    ) {
        if (!validateRegisterInput(name, email, password, phone)) return

        viewModelScope.launch {
            _registerState.value = Resource.Loading
            _registerState.value = authRepository.register(name, email, password, phone, role, badgeId)
        }
    }

    /**
     * Send password-reset OTP to email
     */
    fun forgotPassword(email: String) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _forgotPasswordState.value = Resource.Error("Please enter a valid email address")
            return
        }
        viewModelScope.launch {
            _forgotPasswordState.value = Resource.Loading
            _forgotPasswordState.value = authRepository.forgotPassword(email)
        }
    }

    /**
     * Verify OTP and set a new password
     */
    fun resetPassword(email: String, otp: String, newPassword: String) {
        if (otp.length != 6) {
            _resetPasswordState.value = Resource.Error("Please enter the 6-digit OTP")
            return
        }
        if (newPassword.length < 6) {
            _resetPasswordState.value = Resource.Error("Password must be at least 6 characters")
            return
        }
        viewModelScope.launch {
            _resetPasswordState.value = Resource.Loading
            _resetPasswordState.value = authRepository.resetPassword(email, otp, newPassword)
        }
    }

    /**
     * Check if user is already logged in (for splash screen routing)
     */
    fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoggedIn.value = authRepository.isLoggedIn()
        }
    }

    /**
     * Logout and clear session
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    private fun validateLoginInput(email: String, password: String): Boolean {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginState.value = Resource.Error("Please enter a valid email address")
            return false
        }
        if (password.length < 6) {
            _loginState.value = Resource.Error("Password must be at least 6 characters")
            return false
        }
        return true
    }

    private fun validateRegisterInput(name: String, email: String, password: String, phone: String): Boolean {
        if (name.isBlank()) {
            _registerState.value = Resource.Error("Name is required")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registerState.value = Resource.Error("Invalid email address")
            return false
        }
        if (password.length < 6) {
            _registerState.value = Resource.Error("Password must be at least 6 characters")
            return false
        }
        if (phone.length < 10) {
            _registerState.value = Resource.Error("Enter a valid phone number")
            return false
        }
        return true
    }
}
