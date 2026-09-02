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
import java.util.Date

private const val TAG = "GitHubSubscriptionRepo"

data class SubscriptionInfo(
    val type: String,
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

    // =====================================================
    // ⚠️ اینجا لینکی که از گیت‌هاب گرفتی رو بچسبون
    // =====================================================
    private val CODES_URL = "https://raw.githubusercontent.com/username/aetherst-subscriptions/main/codes.json"

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
                Log.d(TAG, "🔄 در حال دریافت کدها از GitHub...")
                
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
                Log.e(TAG, "❌ خطا در دریافت کدها: ${e.message}")
                cachedCodes ?: throw e
            }
        }
    }

    suspend fun getSubscriptionStatus(): SubscriptionInfo {
        val deviceId = getDeviceId()
        Log.d(TAG, "📡 بررسی اشتراک برای دستگاه: $deviceId")
        
        return try {
            val codes = fetchCodes()
            
            // بررسی کن ببین آیا این دستگاه قبلاً کدی رو فعال کرده
            val found = codes.find { it.deviceId == deviceId && it.used }
            
            if (found != null) {
                val expiresAt = System.currentTimeMillis() + found.durationDays * 24 * 60 * 60 * 1000
                Log.d(TAG, "✅ اشتراک فعال پیدا شد: ${found.durationDays} روز")
                return SubscriptionInfo("paid", expiresAt, true)
            }

            Log.d(TAG, "🆕 دستگاه جدید، Trial ۲۴ ساعته")
            SubscriptionInfo("trial", System.currentTimeMillis() + 24 * 60 * 60 * 1000, true)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ خطا: ${e.message}")
            // در صورت خطا، یک Trial پیش‌فرض برگردون
            SubscriptionInfo("trial", System.currentTimeMillis() + 24 * 60 * 60 * 1000, true)
        }
    }

    suspend fun activateCode(code: String, telegramId: String): ActivationResult {
        val deviceId = getDeviceId()
        Log.d(TAG, "🔑 تلاش برای فعال‌سازی کد: $code")
        
        return try {
            val codes = fetchCodes()
            val found = codes.find { it.code == code }
            
            if (found == null) {
                Log.e(TAG, "❌ کد پیدا نشد: $code")
                return ActivationResult.CodeNotFound
            }

            if (found.used) {
                Log.e(TAG, "❌ کد قبلاً استفاده شده: $code")
                return ActivationResult.CodeAlreadyUsed
            }

            // کد معتبر هست، ولی برای نوشتن توی گیت‌هاب باید دستی این کار رو انجام بدی
            // فعلاً پیام موفقیت برمی‌گردونیم
            Log.d(TAG, "✅ کد معتبر: $code")
            
            // پیشنهاد: می‌تونی یه پیام به کاربر بدی که کد فعال شد و بهش بگی 
            // که تایید نهایی با مدیر هست
            
            ActivationResult.Success
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ خطا: ${e.message}")
            ActivationResult.Error(e.message ?: "Unknown error")
        }
    }
}
