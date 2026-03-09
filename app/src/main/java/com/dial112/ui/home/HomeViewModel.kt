package com.dial112.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.dial112.domain.model.Resource
import com.dial112.domain.model.SosEmergency
import com.dial112.domain.model.User
import com.dial112.domain.repository.AuthRepository
import com.dial112.domain.repository.SosRepository
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HomeViewModel - Manages the Citizen Home screen state
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sosRepository: SosRepository
) : ViewModel() {

    // Current user observable (updates from Room DB cache)
    val currentUser: LiveData<User?> = authRepository.observeCurrentUser().asLiveData()

    // SOS trigger state
    private val _sosState = MutableLiveData<Resource<SosEmergency>>()
    val sosState: LiveData<Resource<SosEmergency>> = _sosState

    fun loadCurrentUser() {
        // currentUser is already a LiveData from Room; no manual load needed
    }

    /**
     * Trigger SOS alert from home screen (after permission grant)
     */
    fun triggerSos(latitude: Double, longitude: Double, address: String) {
        viewModelScope.launch {
            _sosState.value = Resource.Loading
            _sosState.value = sosRepository.triggerSos(latitude, longitude, address)
        }
    }
}
