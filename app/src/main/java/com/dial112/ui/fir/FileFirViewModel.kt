package com.dial112.ui.fir

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dial112.domain.model.Case
import com.dial112.domain.model.ComplaintCategory
import com.dial112.domain.model.Resource
import com.dial112.domain.repository.AiRepository
import com.dial112.domain.repository.CasesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileFirViewModel @Inject constructor(
    private val casesRepository: CasesRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _fileCaseState = MutableLiveData<Resource<Case>>()
    val fileCaseState: LiveData<Resource<Case>> = _fileCaseState

    private val _categoryState = MutableLiveData<Resource<ComplaintCategory>>()
    val categoryState: LiveData<Resource<ComplaintCategory>> = _categoryState

    fun fileCase(
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        imagePath: String?
    ) {
        _fileCaseState.value = Resource.Loading
        viewModelScope.launch {
            _fileCaseState.value = casesRepository.fileCase(title, description, category, latitude, longitude, imagePath)
        }
    }

    fun autoClassify(text: String) {
        if (text.isBlank()) return
        _categoryState.value = Resource.Loading
        viewModelScope.launch {
            _categoryState.value = aiRepository.classifyComplaint(text)
        }
    }
}

