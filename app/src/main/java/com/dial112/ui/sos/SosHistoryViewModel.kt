package com.dial112.ui.sos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dial112.domain.model.Resource
import com.dial112.domain.model.SosLog
import com.dial112.domain.repository.SosManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SosHistoryViewModel @Inject constructor(
    private val sosManagementRepository: SosManagementRepository
) : ViewModel() {

    private val _sosHistory = MutableLiveData<Resource<List<SosLog>>>()
    val sosHistory: LiveData<Resource<List<SosLog>>> = _sosHistory

    private val _activeSos = MutableLiveData<Resource<List<SosLog>>>()
    val activeSos: LiveData<Resource<List<SosLog>>> = _activeSos

    private val _assignResult = MutableLiveData<Resource<String>>()
    val assignResult: LiveData<Resource<String>> = _assignResult

    fun loadHistory() {
        _sosHistory.value = Resource.Loading
        viewModelScope.launch {
            _sosHistory.value = sosManagementRepository.getSosHistory()
        }
    }

    fun loadActiveSos() {
        _activeSos.value = Resource.Loading
        viewModelScope.launch {
            _activeSos.value = sosManagementRepository.getActiveSos()
        }
    }

    fun assignOfficer(sosId: String, officerId: String) {
        _assignResult.value = Resource.Loading
        viewModelScope.launch {
            _assignResult.value = sosManagementRepository.assignOfficer(sosId, officerId)
        }
    }
}
