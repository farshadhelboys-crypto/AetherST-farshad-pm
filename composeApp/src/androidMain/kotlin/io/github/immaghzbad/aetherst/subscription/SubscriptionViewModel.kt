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
        // بارگذاری وضعیت اشتراک هنگام شروع
        loadSubscriptionStatus()
    }

    /**
     * بارگذاری وضعیت اشتراک از کش یا سرور
     */
    fun loadSubscriptionStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val status = repository.getSubscriptionStatus()
                _subscriptionInfo.value = status
                
                // اگر اشتراک منقضی شده، پیام مناسب نمایش بده
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

    /**
     * به‌روزرسانی وضعیت از سرور (با نمایش لودینگ)
     */
    fun refreshStatus() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val status = repository.refreshStatusFromServer()
                _subscriptionInfo.value = status
                
                when {
                    status.isActive -> {
                        _activationMessage.value = "✅ وضعیت اشتراک به‌روز شد"
                    }
                    status.type.startsWith("error") -> {
                        _activationMessage.value = "⚠️ خطا در ارتباط با سرور"
                    }
                    else -> {
                        _activationMessage.value = "ℹ️ اشتراک فعالی یافت نشد"
                    }
                }
            } catch (e: Exception) {
                _activationMessage.value = "❌ خطا در به‌روزرسانی: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * فعال‌سازی کد اشتراک
     */
    fun activateCode(code: String, telegramId: String) {
        // اعتبارسنجی کد
        val trimmedCode = code.trim().uppercase()
        if (trimmedCode.length < 8) {
            _activationMessage.value = "⚠️ کد باید حداقل ۸ کاراکتر باشد"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _activationMessage.value = null
            
            try {
                val result = repository.activateCode(trimmedCode, telegramId.trim())
                
                when (result) {
                    is ActivationResult.Success -> {
                        _activationMessage.value = "✅ اشتراک با موفقیت فعال شد! 🎉"
                        // بارگذاری مجدد وضعیت از سرور
                        val status = repository.refreshStatusFromServer()
                        _subscriptionInfo.value = status
                    }
                    
                    is ActivationResult.CodeNotFound -> {
                        _activationMessage.value = "❌ کد فعال‌سازی نامعتبر است"
                    }
                    
                    is ActivationResult.CodeAlreadyUsed -> {
                        _activationMessage.value = "⚠️ این کد قبلاً استفاده شده است"
                    }
                    
                    is ActivationResult.CodeUsedByOtherDevice -> {
                        _activationMessage.value = "🚫 این کد توسط دستگاه دیگری استفاده می‌شود"
                    }
                    
                    is ActivationResult.Error -> {
                        _activationMessage.value = "❌ خطا: ${result.message}"
                    }
                }
            } catch (e: Exception) {
                _activationMessage.value = "❌ خطا در فعال‌سازی: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * بررسی دستی وضعیت اشتراک (بدون نمایش لودینگ)
     */
    fun checkStatusSilently() {
        viewModelScope.launch {
            try {
                val status = repository.getSubscriptionStatus()
                _subscriptionInfo.value = status
            } catch (e: Exception) {
                // خطا را نادیده بگیر (برای استفاده در پس‌زمینه)
            }
        }
    }

    /**
     * پاک کردن پیام وضعیت
     */
    fun clearMessage() {
        _activationMessage.value = null
    }

    /**
     * پاک کردن کش (برای خروج کاربر یا تست)
     */
    fun clearCache() {
        repository.clearCache()
        _subscriptionInfo.value = null
        _activationMessage.value = "🗑️ اطلاعات کش پاک شد"
    }

    /**
     * دریافت زمان باقیمانده به صورت متن
     */
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

    /**
     * دریافت درصد پیشرفت اشتراک
     */
    fun getProgressPercentage(): Float {
        val info = _subscriptionInfo.value ?: return 0f
        if (!info.isActive) return 0f
        
        val totalDays = 30 // یا از تنظیمات بگیرید
        val remaining = info.expiresAtMillis - System.currentTimeMillis()
        if (remaining <= 0) return 0f
        
        return (remaining / (1000f * 60 * 60 * 24) / totalDays).coerceIn(0f, 1f)
    }

    override fun onCleared() {
        super.onCleared()
        // پاکسازی منابع در صورت نیاز
    }
}
