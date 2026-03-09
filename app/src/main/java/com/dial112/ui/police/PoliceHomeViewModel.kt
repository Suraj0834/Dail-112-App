package com.dial112.ui.police

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dial112.domain.model.Case
import com.dial112.domain.model.Resource
import com.dial112.domain.model.SosEmergency
import com.dial112.domain.model.User
import com.dial112.domain.repository.AuthRepository
import com.dial112.domain.repository.CasesRepository
import com.dial112.domain.repository.SosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PoliceHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val casesRepository: CasesRepository,
    private val sosRepository: SosRepository
) : ViewModel() {

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _assignedCases = MutableLiveData<Resource<List<Case>>>()
    val assignedCases: LiveData<Resource<List<Case>>> = _assignedCases

    private val _sosAlerts = MutableLiveData<Resource<List<SosEmergency>>>(Resource.Success(emptyList()))
    val sosAlerts: LiveData<Resource<List<SosEmergency>>> = _sosAlerts

    init {
        viewModelScope.launch {
            authRepository.observeCurrentUser().collectLatest { user ->
                _currentUser.value = user
            }
        }
        refreshCases()
    }

    fun refreshCases() {
        _assignedCases.value = Resource.Loading
        viewModelScope.launch {
            _assignedCases.value = casesRepository.refreshCases()
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
