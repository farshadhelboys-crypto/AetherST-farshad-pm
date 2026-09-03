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

private const val TAG = "SubscriptionRepo"
private const val CODES_URL = "https://raw.githubusercontent.com/farshadhelboys-crypto/Feri_pm_tunnel_subscriptions/refs/heads/main/codes.json"

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

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
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

        return try {
            val codes = fetchCodes()
            val myActiveCode = codes.find { it.deviceId == deviceId && it.used }

            if (myActiveCode != null) {
                val expiresAt = System.currentTimeMillis() + myActiveCode.durationDays * 24 * 60 * 60 * 1000
                SubscriptionInfo("paid", expiresAt, true)
            } else {
                SubscriptionInfo("none", 0L, false)
            }

        } catch (e: Exception) {
            val detail = describeError(e)
            Log.e(TAG, "getSubscriptionStatus failed: $detail", e)
            SubscriptionInfo("error:$detail", 0L, false)
        }
    }

    suspend fun activateCode(code: String, telegramId: String): ActivationResult {
        Log.d(TAG, "Attempting to activate code: $code")

        return try {
            val deviceId = getDeviceId()
            val codes = fetchCodes()
            val found = codes.find { it.code.equals(code, ignoreCase = true) }

            if (found == null) {
                return ActivationResult.CodeNotFound
            }

            if (found.used && found.deviceId == deviceId) {
                return ActivationResult.Success
            }

            if (found.used) {
                return ActivationResult.CodeUsedByOtherDevice
            }

            ActivationResult.CodeAlreadyUsed

        } catch (e: Exception) {
            val detail = describeError(e)
            Log.e(TAG, "activateCode failed: $detail", e)
            ActivationResult.Error(detail)
        }
    }
}
