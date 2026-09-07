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

private const val PREFS_NAME = "subscription_prefs"
private const val KEY_EXPIRES_AT = "expires_at_millis"
private const val KEY_IS_ACTIVE = "is_active"
private const val KEY_DEVICE_ID = "device_id"
private const val KEY_LAST_CHECK = "last_check_time"
private const val KEY_LICENSE_CODE = "license_code"
private const val TAG = "SubscriptionRepository"

private const val LICENSE_API_URL = "https://aetherst-license-api.farshadhelboys.workers.dev"

private data class ApiResult(val active: Boolean, val expiresAt: Long, val serverTime: Long)

data class SubscriptionInfo(val type: String, val expiresAtMillis: Long, val isActive: Boolean)

sealed class ActivationResult {
    object Success : ActivationResult()
    object CodeNotFound : ActivationResult()
    object CodeAlreadyUsed : ActivationResult()
    object CodeUsedByOtherDevice : ActivationResult()
    object Pending : ActivationResult()
    object NetworkError : ActivationResult()
    data class Error(val message: String) : ActivationResult()
}

class SubscriptionRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        val cached = prefs.getString(KEY_DEVICE_ID, null)
        if (cached != null) return cached
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        Log.d(TAG, "Device ID: $id")
        return id
    }

    private suspend fun request(method: String, endpoint: String, body: JSONObject? = null): ApiResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(LICENSE_API_URL.trimEnd('/') + endpoint)
            Log.d(TAG, "Request: $method $url")
            
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.useCaches = false
            conn.setRequestProperty("Cache-Control", "no-cache, no-store")
            conn.setRequestProperty("Accept", "application/json")
            
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                Log.d(TAG, "Request Body: ${body.toString()}")
            }
            
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
            conn.disconnect()
            
            Log.d(TAG, "Response Code: $code")
            Log.d(TAG, "Response Body: $text")
            
            val json = JSONObject(text.ifBlank { "{}" })
            
            when {
                code == 404 && json.optString("error") == "code_not_found" -> throw CodeNotFoundException()
                code == 409 && json.optString("error") == "code_used_by_other_device" -> throw OtherDeviceException()
                code == 403 && json.optString("error") == "license_revoked" -> throw RevokedException()
                code == 401 -> throw UnauthorizedException()
                code !in 200..299 -> throw Exception(json.optString("error", "HTTP $code"))
            }
            
            val active = json.optBoolean("active", false)
            val expiresAt = json.optLong("expiresAt", 0L)
            val serverTime = json.optLong("serverTime", System.currentTimeMillis())
            
            ApiResult(active, expiresAt, serverTime)
        } catch (e: Exception) {
            Log.e(TAG, "Request error: ${e.message}", e)
            throw e
        }
    }

    private suspend fun statusFromServer(): ApiResult {
        val deviceId = getDeviceId()
        return request("GET", "/v1/status?deviceId=${java.net.URLEncoder.encode(deviceId, "UTF-8")}")
    }

    suspend fun getSubscriptionStatus(): SubscriptionInfo = withContext(Dispatchers.IO) {
        try {
            val r = statusFromServer()
            save(r)
            val isActive = r.active && r.expiresAt > r.serverTime
            SubscriptionInfo(if (r.active) "paid" else "none", r.expiresAt, isActive)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subscription status, using cache: ${e.message}", e)
            val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
            val isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
            val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
            val cacheValid = System.currentTimeMillis() - lastCheck < 24 * 60 * 60 * 1000L
            if (isActive && expiresAt > System.currentTimeMillis() && cacheValid) {
                SubscriptionInfo("paid", expiresAt, true)
            } else {
                SubscriptionInfo("error", 0L, false)
            }
        }
    }

    suspend fun activateCode(code: String, telegramId: String = ""): ActivationResult = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId()
            Log.d(TAG, "Activating code: $code for device: $deviceId")
            
            val r = request("POST", "/v1/activate", JSONObject().apply {
                put("code", code.trim().uppercase())
                put("deviceId", deviceId)
            })
            
            save(r)
            prefs.edit().putString(KEY_LICENSE_CODE, code.trim().uppercase()).apply()
            Log.d(TAG, "Code activated successfully: ${code.trim().uppercase()}")
            ActivationResult.Success
        } catch (_: CodeNotFoundException) { 
            Log.w(TAG, "Code not found: $code")
            ActivationResult.CodeNotFound
        } catch (_: OtherDeviceException) { 
            Log.w(TAG, "Code used by other device: $code")
            ActivationResult.CodeUsedByOtherDevice
        } catch (_: RevokedException) { 
            Log.w(TAG, "Code revoked: $code")
            ActivationResult.Error("این لایسنس توسط مدیر غیرفعال شده است")
        } catch (e: Exception) { 
            Log.e(TAG, "Error activating code: ${e.message}", e)
            ActivationResult.Error(e.message ?: "خطا در ارتباط با سرور") 
        }
    }

    suspend fun extendSubscription(code: String, currentExpiresAt: Long): ActivationResult = activateCode(code, "")

    suspend fun forceRefreshStatus(): SubscriptionInfo = withContext(Dispatchers.IO) {
        try {
            val r = statusFromServer()
            save(r)
            val isActive = r.active && r.expiresAt > r.serverTime
            SubscriptionInfo(if (r.active) "paid" else "none", r.expiresAt, isActive)
        } catch (e: Exception) {
            Log.e(TAG, "Error force refreshing status: ${e.message}", e)
            SubscriptionInfo("error", 0L, false)
        }
    }

    /**
     * استراتژی: اول سرور، در صورت خطای شبکه از کش محلی با اعتبار زمانی استفاده کن.
     * این از قطع شدن لایسنس به خاطر قطع موقت اینترنت یا خطای worker جلوگیری می‌کند.
     */
    suspend fun isConnectionAllowed(): Boolean = withContext(Dispatchers.IO) {
        try {
            val r = statusFromServer()
            save(r)
            val allowed = r.active && r.expiresAt > r.serverTime
            Log.d(TAG, "Connection allowed (server): $allowed")
            allowed
        } catch (e: Exception) {
            Log.e(TAG, "Server unreachable - falling back to cache: ${e.message}")
            val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
            val isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
            val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
            // اعتبار کش تا 24 ساعت بعد از آخرین چک موفق
            val cacheValid = System.currentTimeMillis() - lastCheck < 24 * 60 * 60 * 1000L
            val allowed = isActive && expiresAt > System.currentTimeMillis() && cacheValid
            Log.d(TAG, "Connection allowed (cache): $allowed (active=$isActive, expires=$expiresAt, cacheValid=$cacheValid)")
            allowed
        }
    }

    private fun save(r: ApiResult) {
        prefs.edit().apply {
            putLong(KEY_EXPIRES_AT, r.expiresAt)
            putBoolean(KEY_IS_ACTIVE, r.active && r.expiresAt > r.serverTime)
            putLong(KEY_LAST_CHECK, System.currentTimeMillis())
        }.apply()
        Log.d(TAG, "Subscription data saved: active=${r.active}, expiresAt=${r.expiresAt}")
    }

    fun clearCache() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Subscription cache cleared")
    }
    
    private class CodeNotFoundException : Exception()
    private class OtherDeviceException : Exception()
    private class RevokedException : Exception()
    private class UnauthorizedException : Exception()
}
