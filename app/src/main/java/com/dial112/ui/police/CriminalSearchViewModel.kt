package com.dial112.ui.police

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dial112.domain.model.Criminal
import com.dial112.domain.model.Resource
import com.dial112.domain.repository.CriminalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CriminalSearchViewModel @Inject constructor(
    private val criminalRepository: CriminalRepository
) : ViewModel() {

    private val _criminals = MutableLiveData<Resource<List<Criminal>>>()
    val criminals: LiveData<Resource<List<Criminal>>> = _criminals

    private val _criminalDetail = MutableLiveData<Resource<Criminal>>()
    val criminalDetail: LiveData<Resource<Criminal>> = _criminalDetail

    private var searchJob: Job? = null
    private var currentPage = 1

    fun searchCriminals(query: String = "", debounce: Boolean = true) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(400)
            _criminals.value = Resource.Loading
            _criminals.value = criminalRepository.searchCriminals(query.ifEmpty { null }, currentPage)
        }
    }

    fun loadCriminalDetail(id: String) {
        _criminalDetail.value = Resource.Loading
        viewModelScope.launch {
            _criminalDetail.value = criminalRepository.getCriminalById(id)
        }
    }
}
