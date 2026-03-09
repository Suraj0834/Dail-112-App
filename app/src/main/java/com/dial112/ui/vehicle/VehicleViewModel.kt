package com.dial112.ui.vehicle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dial112.domain.model.Resource
import com.dial112.domain.model.Vehicle
import com.dial112.domain.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _vehicleState = MutableLiveData<Resource<Vehicle>>()
    val vehicleState: LiveData<Resource<Vehicle>> = _vehicleState

    fun searchVehicle(plateNumber: String) {
        if (plateNumber.isBlank()) return
        _vehicleState.value = Resource.Loading
        viewModelScope.launch {
            _vehicleState.value = vehicleRepository.getVehicleByPlate(plateNumber.uppercase().trim())
        }
    }
}
