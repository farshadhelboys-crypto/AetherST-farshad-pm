package io.github.immaghzbad.aetherst.subscription

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "SubscriptionRepo"
private const val CODES_URL = "https://raw.githubusercontent.com/farshadhelboys-crypto/Feri_pm_tunnel_subscriptions/refs/heads/main/codes.json"

// تنظیمات SharedPreferences
private const val PREFS_NAME = "subscription_prefs"
private const val KEY_EXPIRES_AT = "expires_at_millis"
private const val KEY_IS_ACTIVE = "is_active"
private const val KEY_DEVICE_ID = "device_id"
private const val KEY_LAST_CHECK = "last_check_time"

data class SubscriptionInfo(
    val type: String,
    val expiresAtMillis: Long,
    val isActive: Boolean
)

sealed class ActivationResult {
    object Success : ActivationResult()
    object CodeNotFound : ActivationResult()
    object CodeAlreadyUsed : ActivationResult()
    object CodeUsedByOtherDevice : ActivationResult()
    data class Error(val message: String) : ActivationResult()
}

private data class CodeEntry(
    val code: String,
    val deviceId: String,
    val durationDays: Long,
    val used: Boolean
)

class SubscriptionRepository(private val context: Context) {

    // SharedPreferences برای ذخیره‌سازی دائمی
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        // ابتدا از حافظه محلی بخوان
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        
        if (deviceId == null) {
            // اگر وجود نداشت، از سیستم بگیر و ذخیره کن
            deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) 
                ?: "unknown_device"
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        
        return deviceId
    }

    private fun describeError(e: Exception): String {
        val className = e.javaClass.simpleName
        val msg = e.message ?: "no message"
        return "$className: $msg"
    }

    private suspend fun fetchCodes(): List<CodeEntry> = withContext(Dispatchers.IO) {
        val connection = URL(CODES_URL).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("HTTP $responseCode")
        }

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()

        val json = JSONObject(response.toString())
        val codesArray = json.getJSONArray("codes")
        val result = mutableListOf<CodeEntry>()

        for (i in 0 until codesArray.length()) {
            val obj = codesArray.getJSONObject(i)
            result.add(
                CodeEntry(
                    code = obj.getString("code"),
                    deviceId = obj.optString("deviceId", ""),
                    durationDays = obj.getLong("durationDays"),
                    used = obj.getBoolean("used")
                )
            )
        }
        result
    }

    suspend fun getSubscriptionStatus(): SubscriptionInfo {
        val deviceId = getDeviceId()
        Log.d(TAG, "Checking subscription for device: $deviceId")

        // ابتدا از حافظه محلی بخوان
        val savedExpiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val savedIsActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        val currentTime = System.currentTimeMillis()

        // اگر در حافظه محلی ذخیره شده و هنوز معتبر است
        if (savedIsActive && savedExpiresAt > currentTime) {
            Log.d(TAG, "Using cached subscription: expires at $savedExpiresAt")
            return SubscriptionInfo("paid", savedExpiresAt, true)
        }

        // اگر منقضی شده، از سرور بررسی کن
        return try {
            val codes = fetchCodes()
            val myActiveCode = codes.find { it.deviceId == deviceId && it.used }

            if (myActiveCode != null) {
                // تاریخ انقضا را محاسبه کن
                val expiresAt = System.currentTimeMillis() + myActiveCode.durationDays * 24 * 60 * 60 * 1000
                
                // در حافظه محلی ذخیره کن
                prefs.edit().apply {
                    putLong(KEY_EXPIRES_AT, expiresAt)
                    putBoolean(KEY_IS_ACTIVE, true)
                    putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                }.apply()
                
                SubscriptionInfo("paid", expiresAt, true)
            } else {
                // پاک کردن اطلاعات منقضی شده
                prefs.edit().apply {
                    putLong(KEY_EXPIRES_AT, 0L)
                    putBoolean(KEY_IS_ACTIVE, false)
                    putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                }.apply()
                
                SubscriptionInfo("none", 0L, false)
            }

        } catch (e: Exception) {
            val detail = describeError(e)
            Log.e(TAG, "getSubscriptionStatus failed: $detail", e)
            
            // در صورت خطا، اطلاعات ذخیره شده را برگردان (حتی اگر منقضی شده باشد)
            if (savedExpiresAt > 0) {
                SubscriptionInfo("paid", savedExpiresAt, savedExpiresAt > currentTime)
            } else {
                SubscriptionInfo("error:$detail", 0L, false)
            }
        }
    }

    suspend fun activateCode(code: String, telegramId: String): ActivationResult {
        Log.d(TAG, "Attempting to activate code: $code for device: ${getDeviceId()}")

        return try {
            val deviceId = getDeviceId()
            val codes = fetchCodes()
            val found = codes.find { it.code.equals(code, ignoreCase = true) }

            if (found == null) {
                return ActivationResult.CodeNotFound
            }

            // اگر کد قبلاً برای این دستگاه فعال شده
            if (found.used && found.deviceId == deviceId) {
                // ذخیره در حافظه محلی
                val expiresAt = System.currentTimeMillis() + found.durationDays * 24 * 60 * 60 * 1000
                prefs.edit().apply {
                    putLong(KEY_EXPIRES_AT, expiresAt)
                    putBoolean(KEY_IS_ACTIVE, true)
                    putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                }.apply()
                return ActivationResult.Success
            }

            // اگر کد توسط دستگاه دیگری استفاده شده
            if (found.used) {
                return ActivationResult.CodeUsedByOtherDevice
            }

            // کد پیدا شد اما استفاده نشده
            // توجه: در اینجا باید به سرور برای فعال‌سازی درخواست بدهید
            // فعلاً به عنوان AlreadyUsed برگردانده می‌شود تا کاربر Device ID را ارسال کند
            ActivationResult.CodeAlreadyUsed

        } catch (e: Exception) {
            val detail = describeError(e)
            Log.e(TAG, "activateCode failed: $detail", e)
            ActivationResult.Error(detail)
        }
    }

    // تابع برای بررسی دستی وضعیت از سرور و به‌روزرسانی کش
    suspend fun refreshStatusFromServer(): SubscriptionInfo {
        val deviceId = getDeviceId()
        Log.d(TAG, "Refreshing subscription from server for device: $deviceId")

        return try {
            val codes = fetchCodes()
            val myActiveCode = codes.find { it.deviceId == deviceId && it.used }

            if (myActiveCode != null) {
                val expiresAt = System.currentTimeMillis() + myActiveCode.durationDays * 24 * 60 * 60 * 1000
                prefs.edit().apply {
                    putLong(KEY_EXPIRES_AT, expiresAt)
                    putBoolean(KEY_IS_ACTIVE, true)
                    putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                }.apply()
                SubscriptionInfo("paid", expiresAt, true)
            } else {
                prefs.edit().apply {
                    putLong(KEY_EXPIRES_AT, 0L)
                    putBoolean(KEY_IS_ACTIVE, false)
                    putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                }.apply()
                SubscriptionInfo("none", 0L, false)
            }

        } catch (e: Exception) {
            val detail = describeError(e)
            Log.e(TAG, "refreshStatusFromServer failed: $detail", e)
            SubscriptionInfo("error:$detail", 0L, false)
        }
    }

    // تابع برای پاک کردن کش (برای تست یا خروج کاربر)
    fun clearCache() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cache cleared")
    }

    // دریافت زمان آخرین بررسی
    fun getLastCheckTime(): Long {
        return prefs.getLong(KEY_LAST_CHECK, 0L)
    }
}
