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
import java.util.Locale

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
private const val KEY_KNOWN_USED_CODES = "known_used_codes"

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

    private suspend fun fetchCodes(): List<CodeEntry> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val timestamp = System.currentTimeMillis()
            val random = (1000..9999).random()
            val url = URL("$CODES_URL?t=$timestamp&r=$random")

            Log.d(TAG, "Fetching codes from: $url")

            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

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

    /**
     * محاسبه تاریخ انقضای واقعی اشتراک از روی تمام کدهای مصرف‌شده دستگاه.
     *
     * نکته مهم:
     * فقط اولین کد used نباید ملاک باشد؛ چون بعد از تمدید، forceRefresh
     * نباید دوباره تاریخ انقضای کد اول را برگرداند.
     *
     * کدها بر اساس activatedAt مرتب می‌شوند و مدت هر کد به انتهای
     * اشتراک قبلی اضافه می‌شود. اگر کد بعدی بعد از پایان اشتراک قبلی
     * فعال شده باشد، از زمان activatedAt خودش شروع می‌شود.
     */
    private fun calculateSubscriptionExpiry(
        codes: List<CodeEntry>,
        deviceId: String,
        now: Long = System.currentTimeMillis()
    ): Long? {
        val usedCodes = codes
            .filter { it.used && it.deviceId == deviceId }
            .sortedWith(
                compareBy<CodeEntry> {
                    if (it.activatedAt > 0L) it.activatedAt else Long.MAX_VALUE
                }.thenBy { it.code }
            )

        if (usedCodes.isEmpty()) return null

        var expiry: Long? = null

        for (entry in usedCodes) {
            val durationMillis =
                entry.durationDays.coerceAtLeast(0L) * 24L * 60L * 60L * 1000L

            val start = when {
                expiry == null -> {
                    if (entry.activatedAt > 0L) entry.activatedAt else now
                }
                entry.activatedAt > expiry!! -> {
                    entry.activatedAt
                }
                else -> {
                    expiry!!
                }
            }

            expiry = start + durationMillis
        }

        return expiry
    }

    private fun saveActiveSubscription(expiresAt: Long) {
        prefs.edit().apply {
            putLong(KEY_EXPIRES_AT, expiresAt)
            putBoolean(KEY_IS_ACTIVE, expiresAt > System.currentTimeMillis())
            putLong(KEY_LAST_CHECK, System.currentTimeMillis())
        }.apply()
    }

    private fun usedCodeKeys(codes: List<CodeEntry>, deviceId: String): Set<String> =
        codes.filter { it.used && it.deviceId == deviceId }
            .map { it.code.trim().uppercase(Locale.ROOT) }
            .toSet()

    private fun getKnownUsedCodes(): Set<String> =
        prefs.getStringSet(KEY_KNOWN_USED_CODES, emptySet())?.toSet() ?: emptySet()

    private fun saveKnownUsedCodes(keys: Set<String>) {
        prefs.edit().putStringSet(KEY_KNOWN_USED_CODES, keys).apply()
    }

    private fun durationMillis(days: Long): Long =
        days.coerceAtLeast(0L) * 24L * 60L * 60L * 1000L

    private fun calculateInitialExpiry(
        codes: List<CodeEntry>,
        deviceId: String,
        now: Long
    ): Long? {
        val used = codes.filter { it.used && it.deviceId == deviceId }
            .sortedWith(compareBy<CodeEntry> { if (it.activatedAt > 0) it.activatedAt else Long.MAX_VALUE }.thenBy { it.code })
        if (used.isEmpty()) return null
        var expiry = 0L
        for (entry in used) {
            val start = if (expiry == 0L) {
                if (entry.activatedAt > 0L) entry.activatedAt else now
            } else {
                maxOf(expiry, entry.activatedAt)
            }
            expiry = start + durationMillis(entry.durationDays)
        }
        return expiry
    }

    /**
     * Refresh is deliberately non-destructive:
     * - Same set of used codes => keep the locally established expiry exactly as-is.
     * - New used code(s) => append only the duration of the new code(s).
     * - No used codes and no cache => inactive.
     * This prevents a refresh from resurrecting an old subscription or resetting
     * an active timer back to the first code's original expiry.
     */
    private fun reconcileWithServer(
        codes: List<CodeEntry>,
        deviceId: String,
        now: Long
    ): Long? {
        val serverUsed = codes.filter { it.used && it.deviceId == deviceId }
        if (serverUsed.isEmpty()) return null

        val serverKeys = serverUsed.map { it.code.trim().uppercase(Locale.ROOT) }.toSet()
        val knownKeys = getKnownUsedCodes()
        val savedExpiry = prefs.getLong(KEY_EXPIRES_AT, 0L)

        // First sync after an existing local activation: never overwrite its timer.
        if (knownKeys.isEmpty() && savedExpiry > 0L) {
            saveKnownUsedCodes(serverKeys)
            return savedExpiry
        }

        // Nothing changed on the server. Refresh must be completely harmless.
        if (serverKeys == knownKeys && savedExpiry > 0L) {
            return savedExpiry
        }

        val newEntries = serverUsed.filter {
            it.code.trim().uppercase(Locale.ROOT) !in knownKeys
        }

        // New license(s) have appeared. Append them to the current entitlement.
        var expiry = maxOf(savedExpiry, now)
        for (entry in newEntries.sortedWith(compareBy<CodeEntry> { if (it.activatedAt > 0) it.activatedAt else Long.MAX_VALUE }.thenBy { it.code })) {
            expiry += durationMillis(entry.durationDays)
        }

        // If this is a genuinely new installation with no local expiry, build it once.
        if (savedExpiry <= 0L && knownKeys.isEmpty()) {
            return calculateInitialExpiry(codes, deviceId, now)
        }

        saveKnownUsedCodes(serverKeys)
        return expiry
    }

    suspend fun getSubscriptionStatus(): SubscriptionInfo {
        val deviceId = getDeviceId()
        val now = System.currentTimeMillis()
        val savedExpiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)

        return try {
            val codes = fetchCodes()
            val expiry = reconcileWithServer(codes, deviceId, now)

            if (expiry != null) {
                prefs.edit().apply {
                    putLong(KEY_EXPIRES_AT, expiry)
                    putBoolean(KEY_IS_ACTIVE, expiry > now)
                    putLong(KEY_LAST_CHECK, now)
                }.apply()
                SubscriptionInfo("paid", expiry, expiry > now)
            } else {
                // If the server temporarily shows no codes, do not destroy a valid local license.
                if (savedExpiresAt > now) {
                    SubscriptionInfo("paid", savedExpiresAt, true)
                } else {
                    prefs.edit().putBoolean(KEY_IS_ACTIVE, false).putLong(KEY_LAST_CHECK, now).apply()
                    SubscriptionInfo("none", 0L, false)
                }
            }
        } catch (e: Exception) {
            val detail = describeError(e)
            Log.e(TAG, "getSubscriptionStatus failed: $detail", e)
            if (savedExpiresAt > 0L) SubscriptionInfo("paid", savedExpiresAt, savedExpiresAt > now)
            else SubscriptionInfo("error:$detail", 0L, false)
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

            // اگر همین کد قبلاً برای همین دستگاه مصرف شده، فقط وضعیت واقعی
            // کل اشتراک را ذخیره کن و اجازه Reset شدن تایمر را نده.
            if (found.used && found.deviceId == deviceId) {
                val now = System.currentTimeMillis()
                val expiresAt = reconcileWithServer(codes, deviceId, now)

                if (expiresAt != null) {
                    saveActiveSubscription(expiresAt)
                    return ActivationResult.Success
                }

                return ActivationResult.Error("کد مصرف شده است اما وضعیت اشتراک معتبر نیست")
            }

            if (found.used) {
                return ActivationResult.CodeUsedByOtherDevice
            }

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

    suspend fun extendSubscription(
        code: String,
        currentExpiresAt: Long
    ): ActivationResult {
        Log.d(TAG, "Extending subscription with code: $code")

        return try {
            val deviceId = getDeviceId()
            val codes = fetchCodes()
            val found = codes.find { it.code.equals(code, ignoreCase = true) }

            if (found == null) {
                return ActivationResult.CodeNotFound
            }

            if (found.used && found.deviceId == deviceId) {
                // اینجا فقط یک بار entitlement را reconcile می‌کنیم.
                // forceRefreshStatus() بعد از آن همان expiry را حفظ می‌کند و دوباره
                // مدت همین کد را اضافه نمی‌کند.
                val now = System.currentTimeMillis()
                val expiry = reconcileWithServer(codes, deviceId, now)

                if (expiry != null) {
                    saveActiveSubscription(expiry)
                    prefs.edit()
                        .remove(KEY_PENDING_CODE)
                        .remove(KEY_PENDING_TIME)
                        .apply()
                    return ActivationResult.Success
                }

                return ActivationResult.Error("کد تمدید معتبر است اما وضعیت اشتراک قابل محاسبه نیست")
            }

            if (found.used) {
                return ActivationResult.CodeUsedByOtherDevice
            }

            prefs.edit().apply {
                putString(KEY_PENDING_CODE, code)
                putLong(KEY_PENDING_TIME, System.currentTimeMillis())
            }.apply()

            ActivationResult.Pending

        } catch (e: Exception) {
            val detail = describeError(e)
            Log.e(TAG, "extendSubscription failed: $detail", e)
            ActivationResult.Error(detail)
        }
    }

    suspend fun forceRefreshStatus(): SubscriptionInfo {
        val deviceId = getDeviceId()
        var attempts = 0
        var lastError: Exception? = null

        while (attempts < 5) {
            try {
                val now = System.currentTimeMillis()
                val codes = fetchCodes()
                val savedExpiry = prefs.getLong(KEY_EXPIRES_AT, 0L)
                val expiry = reconcileWithServer(codes, deviceId, now)

                if (expiry != null) {
                    prefs.edit().apply {
                        putLong(KEY_EXPIRES_AT, expiry)
                        putBoolean(KEY_IS_ACTIVE, expiry > now)
                        putLong(KEY_LAST_CHECK, now)
                        remove(KEY_PENDING_CODE)
                        remove(KEY_PENDING_TIME)
                    }.apply()
                    return SubscriptionInfo("paid", expiry, expiry > now)
                }

                // A refresh with no NEW license must never erase the current timer.
                if (savedExpiry > 0L) {
                    return SubscriptionInfo("paid", savedExpiry, savedExpiry > now)
                }

                prefs.edit().apply {
                    putBoolean(KEY_IS_ACTIVE, false)
                    putLong(KEY_EXPIRES_AT, 0L)
                    putLong(KEY_LAST_CHECK, now)
                    remove(KEY_PENDING_CODE)
                    remove(KEY_PENDING_TIME)
                }.apply()
                return SubscriptionInfo("none", 0L, false)
            } catch (e: Exception) {
                lastError = e
                attempts++
                if (attempts < 5) delay(300)
            }
        }

        val now = System.currentTimeMillis()
        val cached = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return if (cached > 0L) SubscriptionInfo("paid", cached, cached > now)
        else SubscriptionInfo("error:${lastError?.message ?: "Unknown error"}".take(50), 0L, false)
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
