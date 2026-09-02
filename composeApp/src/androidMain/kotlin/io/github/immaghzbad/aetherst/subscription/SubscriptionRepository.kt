package io.github.immaghzbad.aetherst.subscription

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "GitHubSubscriptionRepo"

data class SubscriptionInfo(
    val type: String,  // "paid" یا "none" یا "error"
    val expiresAtMillis: Long,
    val isActive: Boolean
)

sealed class ActivationResult {
    object Success : ActivationResult()
    object CodeNotFound : ActivationResult()
    object CodeAlreadyUsed : ActivationResult()
    data class Error(val message: String) : ActivationResult()
}

class GitHubSubscriptionRepository(private val context: Context) {

    // ⚠️ لینک گیت‌هاب خودت رو اینجا بچسبون
    private val CODES_URL = "https://raw.githubusercontent.com/farshadhelboys-crypto/Feri_pm_tunnel_subscriptions/refs/heads/main/codes.json"

    private var cachedCodes: MutableList<CodeEntry>? = null
    private var lastFetchTime: Long = 0
    private val CACHE_DURATION = 60_000L // ۱ دقیقه

    private data class CodeEntry(
        val code: String,
        var deviceId: String,
        val durationDays: Long,
        var used: Boolean
    )

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    private suspend fun fetchCodes(): List<CodeEntry> {
        if (cachedCodes != null && System.currentTimeMillis() - lastFetchTime < CACHE_DURATION) {
            return cachedCodes!!
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 دریافت کدها از GitHub...")
                
                val url = URL(CODES_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP Error: $responseCode")
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
                            deviceId = obj.getString("deviceId"),
                            durationDays = obj.getLong("durationDays"),
                            used = obj.getBoolean("used")
                        )
                    )
                }

                cachedCodes = result
                lastFetchTime = System.currentTimeMillis()
                Log.d(TAG, "✅ ${result.size} کد دریافت شد")
                result

            } catch (e: Exception) {
                Log.e(TAG, "❌ خطا: ${e.message}")
                cachedCodes ?: throw e
            }
        }
    }

    suspend fun getSubscriptionStatus(): SubscriptionInfo {
        val deviceId = getDeviceId()
        Log.d(TAG, "📡 بررسی اشتراک برای: $deviceId")
        
        return try {
            val codes = fetchCodes()
            val found = codes.find { it.deviceId == deviceId && it.used }
            
            if (found != null) {
                val expiresAt = System.currentTimeMillis() + found.durationDays * 24 * 60 * 60 * 1000
                Log.d(TAG, "✅ اشتراک فعال: ${found.durationDays} روز")
                return SubscriptionInfo("paid", expiresAt, true)
            }

            Log.d(TAG, "⛔ بدون اشتراک فعال")
            SubscriptionInfo("none", 0L, false)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ خطا: ${e.message}")
            SubscriptionInfo("error", 0L, false)
        }
    }

    suspend fun activateCode(code: String, telegramId: String): ActivationResult {
        val deviceId = getDeviceId()
        Log.d(TAG, "🔑 فعال‌سازی کد: $code")
        
        return try {
            val codes = fetchCodes()
            val found = codes.find { it.code == code }
            
            if (found == null) {
                Log.e(TAG, "❌ کد پیدا نشد")
                return ActivationResult.CodeNotFound
            }

            if (found.used) {
                Log.e(TAG, "❌ کد استفاده شده")
                return ActivationResult.CodeAlreadyUsed
            }

            Log.d(TAG, "✅ کد معتبر: $code")
            ActivationResult.Success
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ خطا: ${e.message}")
            ActivationResult.Error(e.message ?: "Unknown error")
        }
    }
}
