package io.github.immaghzbad.aetherst.subscription

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "SubscriptionVM"

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

    private val _isConnectionAllowed = MutableStateFlow(false)
    val isConnectionAllowed: StateFlow<Boolean> = _isConnectionAllowed.asStateFlow()

    init {
        loadSubscriptionStatus()
    }

    fun loadSubscriptionStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val status = repository.getSubscriptionStatus()
                _subscriptionInfo.value = status
                _isConnectionAllowed.value = status.isActive

                if (status.isActive) {
                    _activationMessage.value = null
                } else {
                    _activationMessage.value = "⚠️ اشتراک شما منقضی شده است. لطفاً آن را تمدید کنید."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading status: ${e.message}", e)
                _activationMessage.value = "❌ خطا در ارتباط با سرور"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch

            _isRefreshing.value = true
            _activationMessage.value = "🔄 در حال به‌روزرسانی..."

            try {
                val status = repository.forceRefreshStatus()
                _subscriptionInfo.value = status
                _isConnectionAllowed.value = status.isActive

                when {
                    status.isActive -> {
                        _activationMessage.value = null
                    }
                    status.type.startsWith("error") -> {
                        _activationMessage.value = "❌ خطا در ارتباط با سرور"
                    }
                    status.type == "pending" -> {
                        _activationMessage.value = "⏳ کد در انتظار تایید است..."
                    }
                    else -> {
                        _activationMessage.value = "ℹ️ اشتراک فعالی یافت نشد"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing: ${e.message}", e)
                _activationMessage.value = "❌ خطا در ارتباط با سرور"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun activateCode(code: String, telegramId: String = "") {
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
                        
                        var attempts = 0
                        var statusUpdated = false
                        while (attempts < 3 && !statusUpdated) {
                            try {
                                delay(500)
                                val status = repository.forceRefreshStatus()
                                _subscriptionInfo.value = status
                                _isConnectionAllowed.value = status.isActive
                                if (status.isActive) {
                                    statusUpdated = true
                                    Log.d(TAG, "Status updated successfully!")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Update attempt $attempts failed", e)
                            }
                            attempts++
                        }
                        
                        delay(1500)
                        _activationMessage.value = null
                        _isLoading.value = false
                    }

                    is ActivationResult.CodeNotFound -> {
                        _activationMessage.value = "❌ کد فعال‌سازی نامعتبر است"
                        delay(2000)
                        _activationMessage.value = null
                        _isLoading.value = false
                    }

                    is ActivationResult.CodeUsedByOtherDevice -> {
                        _activationMessage.value = "🚫 این کد توسط دستگاه دیگری استفاده می‌شود"
                        delay(2000)
                        _activationMessage.value = null
                        _isLoading.value = false
                    }

                    is ActivationResult.Error -> {
                        _activationMessage.value = "❌ خطا: ${result.message}"
                        delay(2000)
                        _activationMessage.value = null
                        _isLoading.value = false
                    }

                    else -> {
                        _activationMessage.value = "❌ خطای ناشناخته"
                        delay(2000)
                        _activationMessage.value = null
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error activating: ${e.message}", e)
                _activationMessage.value = "❌ خطا در فعال‌سازی: ${e.message}"
                delay(2000)
                _activationMessage.value = null
                _isLoading.value = false
            }
        }
    }

    fun checkConnectionWithRetry(
        maxAttempts: Int = 5,
        delayMs: Long = 3000,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            var attempts = 0
            var success = false
            
            while (attempts < maxAttempts && !success) {
                attempts++
                _activationMessage.value = "🔄 تلاش $attempts از $maxAttempts برای اتصال به سرور..."
                
                try {
                    val allowed = repository.isConnectionAllowed()
                    if (allowed) {
                        success = true
                        _isConnectionAllowed.value = true
                        _activationMessage.value = "✅ اتصال برقرار شد"
                        onResult(true)
                    } else {
                        _activationMessage.value = "⏳ لایسنس فعال نیست یا منقضی شده"
                        onResult(false)
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Attempt $attempts failed: ${e.message}")
                    if (attempts < maxAttempts) {
                        delay(delayMs)
                    } else {
                        _activationMessage.value = "❌ پس از $maxAttempts تلاش، سرور پاسخ نداد"
                        _isConnectionAllowed.value = false
                        onResult(false)
                    }
                }
            }
        }
    }

    fun clearMessage() {
        _activationMessage.value = null
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            _subscriptionInfo.value = null
            _isConnectionAllowed.value = false
            _activationMessage.value = "🗑️ اطلاعات کش پاک شد"
            delay(2000)
            _activationMessage.value = null
        }
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

    fun checkAndAllowConnection(): Boolean {
        val allowed = _isConnectionAllowed.value
        Log.d(TAG, "Connection check: $allowed")
        return allowed
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}
