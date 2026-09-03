package io.github.immaghzbad.aetherst.subscription

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SubscriptionRepository(application)

    private val _subscriptionInfo = MutableStateFlow<SubscriptionInfo?>(null)
    val subscriptionInfo: StateFlow<SubscriptionInfo?> = _subscriptionInfo

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _activationMessage = MutableStateFlow<String?>(null)
    val activationMessage: StateFlow<String?> = _activationMessage
    
    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()
    
    private val _showDeviceId = MutableStateFlow(true)
    val showDeviceId: StateFlow<Boolean> = _showDeviceId.asStateFlow()

    init {
        _deviceId.value = repository.getDeviceId()
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val status = repository.getSubscriptionStatus()
                _subscriptionInfo.value = status
                // اگر اشتراک فعال باشد، Device ID را مخفی کن
                _showDeviceId.value = !status.isActive || status.type == "trial"
            } catch (e: Exception) {
                _activationMessage.value = "Connection error: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun activateCode(code: String, telegramId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.activateCode(code.trim(), telegramId.trim())) {
                is ActivationResult.Success -> {
                    _activationMessage.value = "Activated successfully!"
                    refreshStatus()
                }
                is ActivationResult.CodeNotFound -> {
                    _activationMessage.value = "Invalid code."
                }
                is ActivationResult.CodeAlreadyUsed -> {
                    _activationMessage.value = "This code has already been used."
                }
                is ActivationResult.DeviceMismatch -> {
                    _activationMessage.value = "This code is already used on another device."
                }
                is ActivationResult.Error -> {
                    _activationMessage.value = "Error: ${result.message}"
                }
            }
            _isLoading.value = false
        }
    }

    fun clearMessage() {
        _activationMessage.value = null
    }
    
    fun copyDeviceId(): String {
        return _deviceId.value
    }
}
