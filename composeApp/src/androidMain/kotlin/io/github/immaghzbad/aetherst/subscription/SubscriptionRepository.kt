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
private const val KEY_LICENSE_TYPE = "license_type"
private const val KEY_VOLUME_GB = "volume_gb"
private const val KEY_USED_BYTES = "used_bytes"
private const val KEY_REMAINING_BYTES = "remaining_bytes"
private const val KEY_PENDING_DOWNLOAD_BYTES = "pending_download_bytes"
private const val TAG = "SubscriptionRepository"

private const val LICENSE_API_URL = "https://aetherst-license-api.farshadhelboys.workers.dev"

private data class ApiResult(
    val active: Boolean,
    val expiresAt: Long,
    val serverTime: Long,
    val licenseType: String = "time",
    val volumeGb: Double = 0.0,
    val usedBytes: Long = 0L,
    val remainingBytes: Long = 0L
)

/**
 * اطلاعات اشتراک — شامل حجم مصرف‌شده و باقی‌مانده
 */
data class SubscriptionInfo(
    val type: String,
    val expiresAtMillis: Long,
    val isActive: Boolean,
    val licenseType: String = "time",
    val volumeGb: Double = 0.0,
    val usedBytes: Long = 0L,
    val remainingBytes: Long = 0L
) {
    val usedGb: Double get() = usedBytes / (1024.0 * 1024.0 * 1024.0)
    val remainingGb: Double get() = remainingBytes / (1024.0 * 1024.0 * 1024.0)
    val hasVolumeLimit: Boolean get() = licenseType == "volume" || licenseType == "both"
    val volumeExhausted: Boolean get() = hasVolumeLimit && volumeGb > 0 && remainingBytes <= 0
}

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

    private suspend fun request(method: String, endpoint: String, body: JSONObject? = null): ApiResult =
        withContext(Dispatchers.IO) {
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
                    Log.d(TAG, "Request Body: ${body}")
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
                conn.disconnect()

                Log.d(TAG, "Response Code: $code Body: $text")

                val json = JSONObject(text.ifBlank { "{}" })

                when {
                    code == 404 && json.optString("error") == "code_not_found" -> throw CodeNotFoundException()
                    code == 409 && json.optString("error") == "code_used_by_other_device" -> throw OtherDeviceException()
                    code == 403 && json.optString("error") == "license_revoked" -> throw RevokedException()
                    code == 401 -> throw UnauthorizedException()
                    code !in 200..299 -> throw Exception(json.optString("error", "HTTP $code"))
                }

                parseApiResult(json)
            } catch (e: Exception) {
                Log.e(TAG, "Request error: ${e.message}", e)
                throw e
            }
        }

    private fun parseApiResult(json: JSONObject): ApiResult {
        val active = json.optBoolean("active", false)
        val expiresAt = json.optLong("expiresAt", 0L)
        val serverTime = json.optLong("serverTime", System.currentTimeMillis())
        val licenseType = json.optString("licenseType", "time")
        val volumeGb = json.optDouble("volumeGb", 0.0)
        val usedBytes = json.optLong("usedBytes", 0L)
        val remainingBytes = json.optLong("remainingBytes", 0L)
        return ApiResult(active, expiresAt, serverTime, licenseType, volumeGb, usedBytes, remainingBytes)
    }

    private suspend fun statusFromServer(): ApiResult {
        val deviceId = getDeviceId()
        return request("GET", "/v1/status?deviceId=${java.net.URLEncoder.encode(deviceId, "UTF-8")}")
    }

    private fun isEffectivelyActive(r: ApiResult): Boolean {
        if (!r.active) return false
        val type = r.licenseType
        // زمان
        if (type == "time" || type == "both") {
            if (r.expiresAt > 0 && r.expiresAt <= r.serverTime) return false
        }
        // حجم
        if (type == "volume" || type == "both") {
            if (r.volumeGb > 0 && r.remainingBytes <= 0) return false
        }
        return true
    }

    private fun toInfo(r: ApiResult): SubscriptionInfo {
        val active = isEffectivelyActive(r)
        return SubscriptionInfo(
            type = if (active) "paid" else "none",
            expiresAtMillis = r.expiresAt,
            isActive = active,
            licenseType = r.licenseType,
            volumeGb = r.volumeGb,
            usedBytes = r.usedBytes,
            remainingBytes = r.remainingBytes
        )
    }

    suspend fun getSubscriptionStatus(): SubscriptionInfo = withContext(Dispatchers.IO) {
        try {
            val r = statusFromServer()
            save(r)
            toInfo(r)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting status, using cache: ${e.message}", e)
            fromCache()
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
            Log.d(TAG, "Code activated: ${code.trim().uppercase()}")
            ActivationResult.Success
        } catch (_: CodeNotFoundException) {
            ActivationResult.CodeNotFound
        } catch (_: OtherDeviceException) {
            ActivationResult.CodeUsedByOtherDevice
        } catch (_: RevokedException) {
            ActivationResult.Error("این لایسنس توسط مدیر غیرفعال شده است")
        } catch (e: Exception) {
            Log.e(TAG, "Error activating: ${e.message}", e)
            ActivationResult.Error(e.message ?: "خطا در ارتباط با سرور")
        }
    }

    suspend fun extendSubscription(code: String, currentExpiresAt: Long): ActivationResult = activateCode(code, "")

    suspend fun forceRefreshStatus(): SubscriptionInfo = withContext(Dispatchers.IO) {
        try {
            val r = statusFromServer()
            save(r)
            toInfo(r)
        } catch (e: Exception) {
            Log.e(TAG, "Error force refresh: ${e.message}", e)
            SubscriptionInfo("error", 0L, false)
        }
    }

    /**
     * ثبت بایت‌های دانلود شده (محلی) — بعداً با reportPendingUsage به سرور ارسال می‌شود
     */
    fun addDownloadedBytes(bytes: Long) {
        if (bytes <= 0) return
        val pending = prefs.getLong(KEY_PENDING_DOWNLOAD_BYTES, 0L) + bytes
        // به‌روزرسانی تخمینی محلی used/remaining
        val used = prefs.getLong(KEY_USED_BYTES, 0L) + bytes
        val remaining = (prefs.getLong(KEY_REMAINING_BYTES, 0L) - bytes).coerceAtLeast(0L)
        prefs.edit()
            .putLong(KEY_PENDING_DOWNLOAD_BYTES, pending)
            .putLong(KEY_USED_BYTES, used)
            .putLong(KEY_REMAINING_BYTES, remaining)
            .apply()
    }

    /**
     * ارسال مصرف انباشته‌شده به سرور و دریافت وضعیت جدید
     * @return SubscriptionInfo به‌روز؛ اگر حجم تمام شده باشد isActive=false
     */
    suspend fun reportPendingUsage(): SubscriptionInfo = withContext(Dispatchers.IO) {
        val pending = prefs.getLong(KEY_PENDING_DOWNLOAD_BYTES, 0L)
        if (pending <= 0) {
            return@withContext getSubscriptionStatus()
        }
        try {
            val deviceId = getDeviceId()
            val r = request("POST", "/v1/report-usage", JSONObject().apply {
                put("deviceId", deviceId)
                put("deltaBytes", pending)
            })
            // بعد از ارسال موفق، pending را صفر کن
            prefs.edit().putLong(KEY_PENDING_DOWNLOAD_BYTES, 0L).apply()
            save(r)
            toInfo(r)
        } catch (e: Exception) {
            Log.e(TAG, "reportPendingUsage failed: ${e.message}", e)
            // در صورت خطا، مصرف محلی را نگه دار و از کش برگردان
            fromCache()
        }
    }

    /**
     * استراتژی: اول سرور؛ در خطای شبکه از کش (تا ۲۴ ساعت)
     * برای لایسنس حجمی، remainingBytes هم چک می‌شود.
     */
    suspend fun isConnectionAllowed(): Boolean = withContext(Dispatchers.IO) {
        try {
            // قبل از چک، اگر pending زیاد است گزارش بده
            val pending = prefs.getLong(KEY_PENDING_DOWNLOAD_BYTES, 0L)
            if (pending > 256 * 1024) { // بیش از ۲۵۶KB
                reportPendingUsage()
            }
            val r = statusFromServer()
            save(r)
            val allowed = isEffectivelyActive(r)
            Log.d(TAG, "Connection allowed (server): $allowed type=${r.licenseType} rem=${r.remainingBytes}")
            allowed
        } catch (e: Exception) {
            Log.e(TAG, "Server unreachable - cache fallback: ${e.message}")
            val info = fromCache()
            val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
            val cacheValid = System.currentTimeMillis() - lastCheck < 24 * 60 * 60 * 1000L
            val allowed = info.isActive && cacheValid
            Log.d(TAG, "Connection allowed (cache): $allowed")
            allowed
        }
    }

    /** وضعیت محلی فوری (بدون شبکه) — برای UI */
    fun getCachedInfo(): SubscriptionInfo = fromCache()

    private fun fromCache(): SubscriptionInfo {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val isActiveFlag = prefs.getBoolean(KEY_IS_ACTIVE, false)
        val licenseType = prefs.getString(KEY_LICENSE_TYPE, "time") ?: "time"
        val volumeGb = prefs.getFloat(KEY_VOLUME_GB, 0f).toDouble()
        val usedBytes = prefs.getLong(KEY_USED_BYTES, 0L)
        val remainingBytes = prefs.getLong(KEY_REMAINING_BYTES, 0L)
        val now = System.currentTimeMillis()

        var active = isActiveFlag
        if (licenseType == "time" || licenseType == "both") {
            if (expiresAt > 0 && expiresAt <= now) active = false
        }
        if (licenseType == "volume" || licenseType == "both") {
            if (volumeGb > 0 && remainingBytes <= 0) active = false
        }

        return SubscriptionInfo(
            type = if (active) "paid" else "none",
            expiresAtMillis = expiresAt,
            isActive = active,
            licenseType = licenseType,
            volumeGb = volumeGb,
            usedBytes = usedBytes,
            remainingBytes = remainingBytes
        )
    }

    private fun save(r: ApiResult) {
        val active = isEffectivelyActive(r)
        prefs.edit().apply {
            putLong(KEY_EXPIRES_AT, r.expiresAt)
            putBoolean(KEY_IS_ACTIVE, active)
            putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            putString(KEY_LICENSE_TYPE, r.licenseType)
            putFloat(KEY_VOLUME_GB, r.volumeGb.toFloat())
            putLong(KEY_USED_BYTES, r.usedBytes)
            putLong(KEY_REMAINING_BYTES, r.remainingBytes)
        }.apply()
        Log.d(TAG, "Saved: active=$active type=${r.licenseType} used=${r.usedBytes} rem=${r.remainingBytes}")
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
