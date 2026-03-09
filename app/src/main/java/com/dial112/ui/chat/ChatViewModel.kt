package com.dial112.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dial112.domain.model.Resource
import com.dial112.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val sessionId = UUID.randomUUID().toString()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val currentList = _messages.value.orEmpty().toMutableList()
        currentList.add(ChatMessage(text, isUser = true))
        _messages.value = currentList
        _isLoading.value = true

        viewModelScope.launch {
            when (val result = aiRepository.chat(text, sessionId)) {
                is Resource.Success -> {
                    val updated = _messages.value.orEmpty().toMutableList()
                    updated.add(ChatMessage(result.data, isUser = false))
                    _messages.value = updated
                }
                is Resource.Error -> {
                    val updated = _messages.value.orEmpty().toMutableList()
                    updated.add(ChatMessage("Error: ${result.message}", isUser = false))
                    _messages.value = updated
                }
                is Resource.Loading -> { /* ignore */ }
            }
            _isLoading.value = false
        }
    }
}
