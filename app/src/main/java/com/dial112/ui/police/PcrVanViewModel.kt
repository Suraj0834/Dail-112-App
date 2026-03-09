package com.dial112.ui.police

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dial112.domain.model.PcrVan
import com.dial112.domain.model.Resource
import com.dial112.domain.repository.PcrVanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PcrVanViewModel @Inject constructor(
    private val pcrVanRepository: PcrVanRepository
) : ViewModel() {

    private val _allVans = MutableLiveData<Resource<List<PcrVan>>>()
    val allVans: LiveData<Resource<List<PcrVan>>> = _allVans

    private val _myVan = MutableLiveData<Resource<PcrVan?>>()
    val myVan: LiveData<Resource<PcrVan?>> = _myVan

    private val _updateState = MutableLiveData<Resource<PcrVan>>()
    val updateState: LiveData<Resource<PcrVan>> = _updateState

    init {
        loadAllVans()
        loadMyVan()
    }

    fun loadAllVans() {
        _allVans.value = Resource.Loading
        viewModelScope.launch {
            _allVans.value = pcrVanRepository.getAllVans()
        }
    }

    fun loadMyVan() {
        _myVan.value = Resource.Loading
        viewModelScope.launch {
            _myVan.value = pcrVanRepository.getMyVan()
        }
    }

    fun updateVanStatus(vanId: String, status: String, notes: String?) {
        viewModelScope.launch {
            _updateState.value = Resource.Loading
            _updateState.value = pcrVanRepository.updateVan(vanId, status, notes)
            if (_updateState.value is Resource.Success) loadAllVans()
        }
    }
}
