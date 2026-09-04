package io.github.immaghzbad.aetherst.subscription

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
private const val KEY_PENDING_CODE = "pending_code"
private const val KEY_PENDING_TIME = "pending_time"

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
    object Pending : ActivationResult()
    object NetworkError : ActivationResult()
    data class Error(val message: String) : ActivationResult()
}

private data class CodeEntry(
    val code: String,
    val deviceId: String,
    val durationDays: Long,
    val used: Boolean,
    val activatedAt: Long = 0L
)

class SubscriptionRepository(private val context: Context) {

    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
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

    // ⭐ تغییر: اضافه کردن Cache Busting به fetchCodes
    private suspend fun fetchCodes(): List<CodeEntry> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            // ⭐⭐⭐ پارامترهای تصادفی برای شکستن کش ⭐⭐⭐
            val timestamp = System.currentTimeMillis()
            val random = (1000..9999).random()
            val url = URL("$CODES_URL?t=$timestamp&r=$random")
            
            Log.d(TAG, "Fetching codes from: $url")
            
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            // ⭐ هدرهای غیرفعال کردن کش
            connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            connection.setRequestProperty("Pragma", "no-cache")
            connection.setRequestProperty("Expires", "0")

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

            Log.d(TAG, "Response length: ${response.length}")

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
                        used = obj.getBoolean("used"),
                        activatedAt = obj.optLong("activatedAt", 0L)
                    )
                )
            }
            
            Log.d(TAG, "Fetched ${result.size} codes")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "fetchCodes error: ${e.message}", e)
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun getSubscriptionStatus(): SubscriptionInfo {
        val deviceId = getDeviceId()
        Log.d(TAG, "Checking subscription for device: $deviceId")

        val savedExpiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val savedIsActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        val currentTime = System.currentTimeMillis()

        // بررسی کدهای در انتظار
        val pendingCode = prefs.getString(KEY_PENDING_CODE, null)
        if (pendingCode != null) {
            val pendingTime = prefs.getLong(KEY_PENDING_TIME, 0)
            if (System.currentTimeMillis() - pendingTime < 5 * 60 * 1000) {
                return SubscriptionInfo("pending", 0L, false)
            } else {
                prefs.edit().remove(KEY_PENDING_CODE).remove(KEY_PENDING_TIME).apply()
            }
        }

        if (savedIsActive && savedExpiresAt > currentTime) {
            Log.d(TAG, "Using cached subscription: expires at $savedExpiresAt")
            return SubscriptionInfo("paid", savedExpiresAt, true)
        }

        return try {
            val codes = fetchCodes()
            val myActiveCode = codes.find { it.deviceId == deviceId && it.used }

            if (myActiveCode != null) {
                val expiresAt = if (myActiveCode.activatedAt > 0) {
                    myActiveCode.activatedAt + myActiveCode.durationDays * 24 * 60 * 60 * 1000
                } else {
                    System.currentTimeMillis() + myActiveCode.durationDays * 24 * 60 * 60 * 1000
                }
                
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
            Log.e(TAG, "getSubscriptionStatus failed: $detail", e)
            
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

            if (found.used && found.deviceId == deviceId) {
                val expiresAt = System.currentTimeMillis() + found.durationDays * 24 * 60 * 60 * 1000
                prefs.edit().apply {
                    putLong(KEY_EXPIRES_AT, expiresAt)
                    putBoolean(KEY_IS_ACTIVE, true)
                    putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                }.apply()
                return ActivationResult.Success
            }

            if (found.used) {
                return ActivationResult.CodeUsedByOtherDevice
            }

            // کد پیدا شد اما استفاده نشده
            prefs.edit().apply {
                putString(KEY_PENDING_CODE, code)
                putLong(KEY_PENDING_TIME, System.currentTimeMillis())
            }.apply()
            
            ActivationResult.Pending

        } catch (e: Exception) {
            val detail = describeError(e)
            Log.e(TAG, "activateCode failed: $detail", e)
            ActivationResult.Error(detail)
        }
    }

    // ⭐ تغییر: اضافه کردن Force Refresh با Retry
    suspend fun refreshStatusFromServer(): SubscriptionInfo {
        val deviceId = getDeviceId()
        Log.d(TAG, "Refreshing subscription from server for device: $deviceId")

        var attempts = 0
        var lastError: Exception? = null

        while (attempts < 3) {
            try {
                val codes = fetchCodes()
                val myActiveCode = codes.find { it.deviceId == deviceId && it.used }

                if (myActiveCode != null) {
                    val expiresAt = if (myActiveCode.activatedAt > 0) {
                        myActiveCode.activatedAt + myActiveCode.durationDays * 24 * 60 * 60 * 1000
                    } else {
                        System.currentTimeMillis() + myActiveCode.durationDays * 24 * 60 * 60 * 1000
                    }
                    
                    prefs.edit().apply {
                        putLong(KEY_EXPIRES_AT, expiresAt)
                        putBoolean(KEY_IS_ACTIVE, true)
                        putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                        remove(KEY_PENDING_CODE)
                        remove(KEY_PENDING_TIME)
                    }.apply()
                    return SubscriptionInfo("paid", expiresAt, true)
                } else {
                    prefs.edit().apply {
                        putLong(KEY_EXPIRES_AT, 0L)
                        putBoolean(KEY_IS_ACTIVE, false)
                        putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                    }.apply()
                    return SubscriptionInfo("none", 0L, false)
                }

            } catch (e: Exception) {
                lastError = e
                Log.e(TAG, "Refresh attempt ${attempts + 1} failed: ${e.message}", e)
                attempts++
                if (attempts < 3) {
                    delay(500)
                }
            }
        }

        val detail = lastError?.let { describeError(it) } ?: "Unknown error"
        return SubscriptionInfo("error:$detail", 0L, false)
    }

    // ⭐ تابع جدید: Force Refresh با پارامترهای تصادفی
    suspend fun forceRefreshStatus(): SubscriptionInfo {
        val deviceId = getDeviceId()
        Log.d(TAG, "Force refreshing from server for device: $deviceId")
        
        var attempts = 0
        var lastError: Exception? = null
        
        while (attempts < 5) {
            try {
                val codes = fetchCodes()
                val myActiveCode = codes.find { it.deviceId == deviceId && it.used }

                if (myActiveCode != null) {
                    val expiresAt = if (myActiveCode.activatedAt > 0) {
                        myActiveCode.activatedAt + myActiveCode.durationDays * 24 * 60 * 60 * 1000
                    } else {
                        System.currentTimeMillis() + myActiveCode.durationDays * 24 * 60 * 60 * 1000
                    }
                    
                    prefs.edit().apply {
                        putLong(KEY_EXPIRES_AT, expiresAt)
                        putBoolean(KEY_IS_ACTIVE, true)
                        putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                        remove(KEY_PENDING_CODE)
                        remove(KEY_PENDING_TIME)
                    }.apply()
                    return SubscriptionInfo("paid", expiresAt, true)
                } else {
                    prefs.edit().apply {
                        putLong(KEY_EXPIRES_AT, 0L)
                        putBoolean(KEY_IS_ACTIVE, false)
                        putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                        remove(KEY_PENDING_CODE)
                        remove(KEY_PENDING_TIME)
                    }.apply()
                    return SubscriptionInfo("none", 0L, false)
                }
                
            } catch (e: Exception) {
                lastError = e
                Log.e(TAG, "Force refresh attempt ${attempts + 1} failed: ${e.message}", e)
                attempts++
                if (attempts < 5) {
                    delay(300)
                }
            }
        }
        
        val detail = lastError?.let { describeError(it) } ?: "Unknown error"
        return SubscriptionInfo("error:$detail".take(50), 0L, false)
    }

    fun clearCache() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cache cleared")
    }

    fun getLastCheckTime(): Long {
        return prefs.getLong(KEY_LAST_CHECK, 0L)
    }
    
    fun isPending(): Boolean {
        return prefs.getString(KEY_PENDING_CODE, null) != null
    }
}
