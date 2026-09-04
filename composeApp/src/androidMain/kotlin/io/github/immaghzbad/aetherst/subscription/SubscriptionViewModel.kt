package io.github.immaghzbad.aetherst.subscription

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SubscriptionRepository(application)
    
    val deviceId: String get() = repository.getDeviceId()

    private val _subscriptionInfo = MutableStateFlow<SubscriptionInfo?>(null)
    val subscriptionInfo: StateFlow<SubscriptionInfo?> = _subscriptionInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _activationMessage = MutableStateFlow<String?>(null)
    val activationMessage: StateFlow<String?> = _activationMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadSubscriptionStatus()
    }

    fun loadSubscriptionStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val status = repository.getSubscriptionStatus()
                _subscriptionInfo.value = status
                
                if (!status.isActive && status.type != "error") {
                    _activationMessage.value = "⚠️ اشتراک شما منقضی شده است. لطفاً آن را تمدید کنید."
                }
            } catch (e: Exception) {
                _activationMessage.value = "❌ خطا در بارگذاری: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ⭐ تغییر: استفاده از forceRefreshStatus
    fun refreshStatus() {
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch
            
            _isRefreshing.value = true
            _activationMessage.value = "🔄 در حال به‌روزرسانی..."
            
            try {
                val status = repository.forceRefreshStatus()
                _subscriptionInfo.value = status
                
                when {
                    status.isActive -> {
                        _activationMessage.value = "✅ وضعیت اشتراک به‌روز شد"
                    }
                    status.type.startsWith("error") -> {
                        _activationMessage.value = "⚠️ خطا در ارتباط با سرور"
                    }
                    status.type == "pending" -> {
                        _activationMessage.value = "⏳ کد در انتظار تایید است..."
                    }
                    else -> {
                        _activationMessage.value = "ℹ️ اشتراک فعالی یافت نشد"
                    }
                }
            } catch (e: Exception) {
                _activationMessage.value = "❌ خطا در به‌روزرسانی: ${e.message}"
            } finally {
                _isRefreshing.value = false
                delay(3000)
                if (_activationMessage.value != null) {
                    _activationMessage.value = null
                }
            }
        }
    }

    fun activateCode(code: String, telegramId: String) {
        val trimmedCode = code.trim().uppercase()
        if (trimmedCode.length < 8) {
            _activationMessage.value = "⚠️ کد باید حداقل ۸ کاراکتر باشد"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _activationMessage.value = "🔄 در حال بررسی کد..."
            
            try {
                val result = repository.activateCode(trimmedCode, telegramId.trim())
                
                when (result) {
                    is ActivationResult.Success -> {
                        _activationMessage.value = "✅ اشتراک با موفقیت فعال شد! 🎉"
                        delay(500)
                        val status = repository.forceRefreshStatus()
                        _subscriptionInfo.value = status
                        delay(2000)
                        _activationMessage.value = null
                    }
                    
                    is ActivationResult.Pending -> {
                        _activationMessage.value = "⏳ کد برای فعال‌سازی ارسال شد. پس از تایید ادمین، Refresh بزنید."
                    }
                    
                    is ActivationResult.CodeNotFound -> {
                        _activationMessage.value = "❌ کد فعال‌سازی نامعتبر است"
                        delay(3000)
                        _activationMessage.value = null
                    }
                    
                    is ActivationResult.CodeAlreadyUsed -> {
                        _activationMessage.value = "⚠️ این کد قبلاً استفاده شده است"
                        delay(3000)
                        _activationMessage.value = null
                    }
                    
                    is ActivationResult.CodeUsedByOtherDevice -> {
                        _activationMessage.value = "🚫 این کد توسط دستگاه دیگری استفاده می‌شود"
                        delay(3000)
                        _activationMessage.value = null
                    }
                    
                    is ActivationResult.NetworkError -> {
                        _activationMessage.value = "⚠️ خطای شبکه. دوباره تلاش کنید"
                        delay(3000)
                        _activationMessage.value = null
                    }
                    
                    is ActivationResult.Error -> {
                        _activationMessage.value = "❌ خطا: ${result.message}"
                        delay(3000)
                        _activationMessage.value = null
                    }
                }
            } catch (e: Exception) {
                _activationMessage.value = "❌ خطا در فعال‌سازی: ${e.message}"
                delay(3000)
                _activationMessage.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkStatusSilently() {
        viewModelScope.launch {
            try {
                val status = repository.getSubscriptionStatus()
                _subscriptionInfo.value = status
            } catch (e: Exception) {
                // خطا را نادیده بگیر
            }
        }
    }

    fun clearMessage() {
        _activationMessage.value = null
    }

    fun clearCache() {
        repository.clearCache()
        _subscriptionInfo.value = null
        _activationMessage.value = "🗑️ اطلاعات کش پاک شد"
        delay(2000)
        _activationMessage.value = null
    }

    fun getRemainingTimeText(): String {
        val info = _subscriptionInfo.value ?: return "نامشخص"
        if (!info.isActive) return "منقضی شده"
        
        val remaining = info.expiresAtMillis - System.currentTimeMillis()
        if (remaining <= 0) return "منقضی شده"
        
        val days = remaining / (1000 * 60 * 60 * 24)
        val hours = (remaining / (1000 * 60 * 60)) % 24
        val minutes = (remaining / (1000 * 60)) % 60
        
        return when {
            days > 0 -> "$days روز و $hours ساعت"
            hours > 0 -> "$hours ساعت و $minutes دقیقه"
            else -> "$minutes دقیقه"
        }
    }

    fun getProgressPercentage(): Float {
        val info = _subscriptionInfo.value ?: return 0f
        if (!info.isActive) return 0f
        
        val totalDays = 30
        val remaining = info.expiresAtMillis - System.currentTimeMillis()
        if (remaining <= 0) return 0f
        
        return (remaining / (1000f * 60 * 60 * 24) / totalDays).coerceIn(0f, 1f)
    }

    override fun onCleared() {
        super.onCleared()
    }
}
