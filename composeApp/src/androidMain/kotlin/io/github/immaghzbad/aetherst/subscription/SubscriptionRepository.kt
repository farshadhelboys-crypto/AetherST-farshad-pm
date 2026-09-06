package io.github.immaghzbad.aetherst.subscription

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
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

// بعد از Deploy کردن Cloudflare Worker، فقط این آدرس را با آدرس Worker خودت عوض کن.
private const val LICENSE_API_URL = "https://REPLACE_WITH_YOUR_WORKER.workers.dev"

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
        return id
    }

    private suspend fun request(method: String, endpoint: String, body: JSONObject? = null): ApiResult = withNetwork {
        val conn = (URL(LICENSE_API_URL.trimEnd('/') + endpoint).openConnection() as HttpURLConnection)
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
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        conn.disconnect()
        val json = JSONObject(text.ifBlank { "{}" })
        if (code == 404 && json.optString("error") == "code_not_found") throw CodeNotFoundException()
        if (code == 409 && json.optString("error") == "code_used_by_other_device") throw OtherDeviceException()
        if (code == 403 && json.optString("error") == "license_revoked") throw RevokedException()
        if (code !in 200..299) throw Exception(json.optString("error", "HTTP $code"))
        ApiResult(json.optBoolean("active", false), json.optLong("expiresAt", 0L), json.optLong("serverTime", System.currentTimeMillis()))
    }

    private suspend fun statusFromServer(): ApiResult =
        request("GET", "/v1/status?deviceId=${java.net.URLEncoder.encode(getDeviceId(), "UTF-8")}")

    suspend fun getSubscriptionStatus(): SubscriptionInfo = withContext(Dispatchers.IO) {
        try {
            val r = statusFromServer()
            save(r)
            SubscriptionInfo(if (r.active) "paid" else "none", r.expiresAt, r.active && r.expiresAt > r.serverTime)
        } catch (e: Exception) {
            val exp = prefs.getLong(KEY_EXPIRES_AT, 0L)
            val active = prefs.getBoolean(KEY_IS_ACTIVE, false) && exp > System.currentTimeMillis()
            SubscriptionInfo(if (active) "paid_cached" else "error", exp, active)
        }
    }

    suspend fun activateCode(code: String, telegramId: String): ActivationResult = withContext(Dispatchers.IO) {
        try {
            val r = request("POST", "/v1/activate", JSONObject().apply {
                put("code", code.trim().uppercase())
                put("deviceId", getDeviceId())
            })
            save(r)
            prefs.edit().putString(KEY_LICENSE_CODE, code.trim().uppercase()).apply()
            ActivationResult.Success
        } catch (_: CodeNotFoundException) { ActivationResult.CodeNotFound
        } catch (_: OtherDeviceException) { ActivationResult.CodeUsedByOtherDevice
        } catch (_: RevokedException) { ActivationResult.Error("این لایسنس توسط مدیر غیرفعال شده است")
        } catch (e: Exception) { ActivationResult.Error(e.message ?: "خطا در ارتباط با سرور") }
    }

    suspend fun extendSubscription(code: String, currentExpiresAt: Long): ActivationResult = activateCode(code, "")

    suspend fun forceRefreshStatus(): SubscriptionInfo = withContext(Dispatchers.IO) {
        try {
            val r = statusFromServer()
            save(r)
            SubscriptionInfo(if (r.active) "paid" else "none", r.expiresAt, r.active && r.expiresAt > r.serverTime)
        } catch (e: Exception) {
            val exp = prefs.getLong(KEY_EXPIRES_AT, 0L)
            SubscriptionInfo("error", exp, false)
        }
    }

    suspend fun isConnectionAllowed(): Boolean = withContext(Dispatchers.IO) {
        try {
            val r = statusFromServer()
            save(r)
            r.active && r.expiresAt > r.serverTime
        } catch (_: Exception) {
            val exp = prefs.getLong(KEY_EXPIRES_AT, 0L)
            val last = prefs.getLong(KEY_LAST_CHECK, 0L)
            // فقط 24 ساعت Grace برای قطعی موقت اینترنت؛ زمان انقضا همچنان محلی چک می‌شود.
            prefs.getBoolean(KEY_IS_ACTIVE, false) && exp > System.currentTimeMillis() && System.currentTimeMillis() - last <= 86_400_000L
        }
    }

    private fun save(r: ApiResult) {
        prefs.edit().putLong(KEY_EXPIRES_AT, r.expiresAt).putBoolean(KEY_IS_ACTIVE, r.active && r.expiresAt > r.serverTime).putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
    }

    private suspend fun <T> withNetwork(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    fun clearCache() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Subscription cache cleared")
    }
    private class CodeNotFoundException : Exception()
    private class OtherDeviceException : Exception()
    private class RevokedException : Exception()
}
