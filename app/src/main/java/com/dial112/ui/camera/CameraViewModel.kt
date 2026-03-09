package com.dial112.ui.camera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dial112.domain.model.FaceMatch
import com.dial112.domain.model.Resource
import com.dial112.domain.model.WeaponDetection
import com.dial112.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _faceResult = MutableLiveData<Resource<FaceMatch>>()
    val faceResult: LiveData<Resource<FaceMatch>> = _faceResult

    private val _weaponResult = MutableLiveData<Resource<WeaponDetection>>()
    val weaponResult: LiveData<Resource<WeaponDetection>> = _weaponResult

    private val _anprResult = MutableLiveData<Resource<String>>()
    val anprResult: LiveData<Resource<String>> = _anprResult

    fun recognizeFace(imageBytes: ByteArray) {
        _faceResult.value = Resource.Loading
        viewModelScope.launch {
            _faceResult.value = aiRepository.recognizeFace(imageBytes)
        }
    }

    fun detectWeapon(imageBytes: ByteArray) {
        _weaponResult.value = Resource.Loading
        viewModelScope.launch {
            _weaponResult.value = aiRepository.detectWeapon(imageBytes)
        }
    }

    fun detectPlate(imageBytes: ByteArray) {
        _anprResult.value = Resource.Loading
        viewModelScope.launch {
            _anprResult.value = aiRepository.detectNumberPlate(imageBytes)
        }
    }
}
