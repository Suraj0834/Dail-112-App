package com.dial112.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dial112.domain.model.Hotspot
import com.dial112.domain.model.Resource
import com.dial112.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrimeMapViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _hotspots = MutableLiveData<Resource<List<Hotspot>>>()
    val hotspots: LiveData<Resource<List<Hotspot>>> = _hotspots

    fun loadHotspots() {
        _hotspots.value = Resource.Loading
        viewModelScope.launch {
            _hotspots.value = aiRepository.getCrimeHotspots()
        }
    }
}
