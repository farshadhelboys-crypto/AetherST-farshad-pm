package io.github.immaghzbad.aetherst.subscription

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SubscriptionRepository(application)

    private val _subscriptionInfo = MutableStateFlow<SubscriptionInfo?>(null)
    val subscriptionInfo: StateFlow<SubscriptionInfo?> = _subscriptionInfo

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _activationMessage = MutableStateFlow<String?>(null)
    val activationMessage: StateFlow<String?> = _activationMessage

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _subscriptionInfo.value = repository.getSubscriptionStatus()
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
                    _activationMessage.value = "با موفقیت فعال سازی انجام شد !"
                    refreshStatus()
                }
                is ActivationResult.CodeNotFound -> {
                    _activationMessage.value = "نامعتبر ."
                }
                is ActivationResult.CodeAlreadyUsed -> {
                    _activationMessage.value = "این کد قبلا استفاده شده است ."
                }
                is ActivationResult.CodeUsedByOtherDevice -> {
                    _activationMessage.value = "این کد در دستگاه دیگری استفاده شده است ."
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
}
