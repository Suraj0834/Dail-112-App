package com.dial112.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dial112.domain.model.Resource
import com.dial112.domain.model.User
import com.dial112.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profile = MutableLiveData<Resource<User>>()
    val profile: LiveData<Resource<User>> = _profile

    private val _updateResult = MutableLiveData<Resource<User>>()
    val updateResult: LiveData<Resource<User>> = _updateResult

    fun loadProfile() {
        _profile.value = Resource.Loading
        viewModelScope.launch {
            _profile.value = profileRepository.getProfile()
        }
    }

    fun updateProfile(name: String?, phone: String?) {
        _updateResult.value = Resource.Loading
        viewModelScope.launch {
            _updateResult.value = profileRepository.updateProfile(name, phone)
        }
    }
}
