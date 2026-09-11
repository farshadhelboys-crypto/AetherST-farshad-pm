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

    private val _showPurchaseRequired = MutableStateFlow(false)
    val showPurchaseRequired: StateFlow<Boolean> = _showPurchaseRequired.asStateFlow()

    init {
        loadSubscriptionStatus()
        startUsageReporter()
    }

    fun loadSubscriptionStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val status = repository.getSubscriptionStatus()
                applyStatus(status)
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
                applyStatus(status)
                when {
                    status.isActive -> _activationMessage.value = null
                    status.type.startsWith("error") -> _activationMessage.value = "❌ خطا در ارتباط با سرور"
                    status.volumeExhausted ->
                        _activationMessage.value = "📉 حجم اشتراک شما تمام شده است. لطفاً اشتراک جدید خریداری کنید."
                    else -> _activationMessage.value = "ℹ️ اشتراک فعالی یافت نشد"
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
        if (trimmedCode.length < 4) {
            _activationMessage.value = "⚠️ کد معتبر نیست"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _activationMessage.value = "⏳ در حال فعال‌سازی..."
            when (val result = repository.activateCode(trimmedCode, telegramId)) {
                is ActivationResult.Success -> {
                    val status = repository.getSubscriptionStatus()
                    applyStatus(status)
                    _activationMessage.value = "✅ لایسنس با موفقیت فعال شد"
                    delay(1500)
                    _activationMessage.value = null
                }
                is ActivationResult.CodeNotFound -> {
                    _activationMessage.value = "❌ کد لایسنس یافت نشد"
                }
                is ActivationResult.CodeUsedByOtherDevice -> {
                    _activationMessage.value = "❌ این کد روی دستگاه دیگری فعال است"
                }
                is ActivationResult.Error -> {
                    _activationMessage.value = "❌ ${result.message}"
                }
                else -> {
                    _activationMessage.value = "❌ خطا در فعال‌سازی"
                }
            }
            _isLoading.value = false
        }
    }

    fun extendSubscription(code: String) {
        val trimmedCode = code.trim().uppercase()
        if (trimmedCode.length < 4) {
            _activationMessage.value = "⚠️ کد معتبر نیست"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _activationMessage.value = "🔄 در حال تمدید اشتراک..."
            try {
                val currentExpires = _subscriptionInfo.value?.expiresAtMillis ?: 0L
                when (val result = repository.extendSubscription(trimmedCode, currentExpires)) {
                    is ActivationResult.Success -> {
                        _activationMessage.value = "✅ اشتراک با موفقیت تمدید شد! 🎉"
                        val status = repository.forceRefreshStatus()
                        applyStatus(status)
                        delay(1500)
                        _activationMessage.value = null
                    }
                    is ActivationResult.CodeNotFound -> {
                        _activationMessage.value = "❌ کد تمدید نامعتبر است"
                    }
                    is ActivationResult.CodeUsedByOtherDevice -> {
                        _activationMessage.value = "🚫 این کد توسط دستگاه دیگری استفاده می‌شود"
                    }
                    is ActivationResult.Error -> {
                        _activationMessage.value = "❌ ${result.message}"
                    }
                    else -> {
                        _activationMessage.value = "❌ خطا در تمدید"
                    }
                }
            } catch (e: Exception) {
                _activationMessage.value = "❌ ${e.message ?: "خطا"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onDownloadBytes(bytes: Long) {
        if (bytes <= 0) return
        repository.addDownloadedBytes(bytes)
        val cached = repository.getCachedInfo()
        _subscriptionInfo.value = cached
        if (cached.volumeExhausted || !cached.isActive) {
            _isConnectionAllowed.value = false
            _showPurchaseRequired.value = true
            _activationMessage.value = "📉 حجم اشتراک تمام شد. لطفاً اشتراک جدید بخرید."
        }
    }

    fun flushUsageAndCheck() {
        viewModelScope.launch {
            try {
                val status = repository.reportPendingUsage()
                applyStatus(status)
            } catch (e: Exception) {
                Log.e(TAG, "flushUsage failed: ${e.message}")
            }
        }
    }

    fun checkConnectionAllowed(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val allowed = repository.isConnectionAllowed()
            _isConnectionAllowed.value = allowed
            if (!allowed) {
                val info = repository.getCachedInfo()
                _showPurchaseRequired.value = true
                _activationMessage.value = if (info.volumeExhausted) {
                    "📉 حجم اشتراک تمام شد. لطفاً اشتراک جدید بخرید."
                } else {
                    "⚠️ اشتراک شما منقضی شده است."
                }
            }
            onResult(allowed)
        }
    }

    fun checkConnectionWithRetry(
        maxAttempts: Int = 3,
        delayMs: Long = 1500L,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            var attempts = 0
            while (attempts < maxAttempts) {
                attempts++
                try {
                    val allowed = repository.isConnectionAllowed()
                    if (allowed) {
                        _isConnectionAllowed.value = true
                        _activationMessage.value = "✅ اتصال برقرار شد"
                        onResult(true)
                        return@launch
                    } else {
                        _activationMessage.value = "⏳ لایسنس فعال نیست یا منقضی شده"
                        onResult(false)
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Attempt $attempts failed: ${e.message}")
                    if (attempts < maxAttempts) delay(delayMs)
                    else {
                        _activationMessage.value = "❌ پس از $maxAttempts تلاش، سرور پاسخ نداد"
                        _isConnectionAllowed.value = false
                        onResult(false)
                    }
                }
            }
        }
    }

    fun checkAndAllowConnection(): Boolean {
        val allowed = _isConnectionAllowed.value
        Log.d(TAG, "Connection check: $allowed")
        return allowed
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

    fun dismissPurchasePrompt() {
        _showPurchaseRequired.value = false
    }

    fun getRemainingTimeText(): String {
        val info = _subscriptionInfo.value ?: return "نامشخص"
        if (!info.isActive) return "منقضی شده"
        if (info.licenseType == "volume") return "نامحدود زمانی"
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
        if (info.hasVolumeLimit && info.volumeGb > 0) {
            return (1f - (info.usedGb / info.volumeGb).toFloat()).coerceIn(0f, 1f)
        }
        val totalDays = 30
        val remaining = info.expiresAtMillis - System.currentTimeMillis()
        if (remaining <= 0) return 0f
        return (remaining / (1000f * 60 * 60 * 24) / totalDays).coerceIn(0f, 1f)
    }

    fun formatVolumeUsed(): String {
        val info = _subscriptionInfo.value ?: return "—"
        if (!info.hasVolumeLimit) return "بدون محدودیت حجم"
        return String.format("%.2f / %.1f GB", info.usedGb, info.volumeGb)
    }

    fun formatVolumeRemaining(): String {
        val info = _subscriptionInfo.value ?: return "—"
        if (!info.hasVolumeLimit) return "—"
        return String.format("%.2f GB", info.remainingGb.coerceAtLeast(0.0))
    }

    private fun applyStatus(status: SubscriptionInfo) {
        _subscriptionInfo.value = status
        _isConnectionAllowed.value = status.isActive
        _showPurchaseRequired.value = !status.isActive
        if (!status.isActive) {
            if (status.volumeExhausted) {
                _activationMessage.value = "📉 حجم اشتراک شما تمام شده است. لطفاً اشتراک جدید خریداری کنید."
            } else if (_activationMessage.value == null) {
                _activationMessage.value = "⚠️ اشتراک شما منقضی شده است. لطفاً آن را تمدید کنید."
            }
        }
    }

    private fun startUsageReporter() {
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                try {
                    val status = repository.reportPendingUsage()
                    applyStatus(status)
                } catch (e: Exception) {
                    Log.w(TAG, "Periodic report failed: ${e.message}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}
