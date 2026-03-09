package com.dial112.ui.cases

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.dial112.domain.model.Case
import com.dial112.domain.model.Resource
import com.dial112.domain.repository.CasesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * CasesViewModel - Manages case listing and filing
 */
@HiltViewModel
class CasesViewModel @Inject constructor(
    private val casesRepository: CasesRepository
) : ViewModel() {

    // Observable cases from Room (updates automatically)
    val cases: LiveData<List<Case>> = casesRepository.observeCases().asLiveData()

    // File case state
    private val _fileCaseState = MutableLiveData<Resource<Case>>()
    val fileCaseState: LiveData<Resource<Case>> = _fileCaseState

    // Single case detail
    private val _caseDetail = MutableLiveData<Resource<Case>>()
    val caseDetail: LiveData<Resource<Case>> = _caseDetail

    // Refresh state
    private val _refreshState = MutableLiveData<Resource<List<Case>>>()
    val refreshState: LiveData<Resource<List<Case>>> = _refreshState

    fun refreshCases() {
        viewModelScope.launch {
            _refreshState.value = Resource.Loading
            _refreshState.value = casesRepository.refreshCases()
        }
    }

    fun fileCase(
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        imagePath: String?
    ) {
        viewModelScope.launch {
            _fileCaseState.value = Resource.Loading
            _fileCaseState.value = casesRepository.fileCase(
                title, description, category, latitude, longitude, imagePath
            )
        }
    }

    fun getCaseDetail(caseId: String) {
        viewModelScope.launch {
            _caseDetail.value = Resource.Loading
            _caseDetail.value = casesRepository.getCaseById(caseId)
        }
    }

    fun updateCase(caseId: String, status: String, notes: String?) {
        viewModelScope.launch {
            _caseDetail.value = Resource.Loading
            _caseDetail.value = casesRepository.updateCase(caseId, status, notes)
        }
    }
}
