package io.github.immaghzbad.aetherst.subscription

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

private const val TAG = "SubscriptionRepo"

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

class SubscriptionRepository(private val context: Context) {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    private fun describeError(e: Exception): String {
        val className = e.javaClass.simpleName
        val msg = e.message ?: "no message"
        val causeClass = e.cause?.javaClass?.simpleName ?: "none"
        val causeMsg = e.cause?.message ?: ""
        return "$className: $msg | cause: $causeClass - $causeMsg"
    }

    suspend fun getSubscriptionStatus(): SubscriptionInfo {
        val deviceId = getDeviceId()
        Log.d(TAG, "Checking subscription for device: $deviceId")

        return try {
            val doc = db.collection("subscriptions").document(deviceId).get().await()

            if (!doc.exists()) {
                Log.d(TAG, "No record found, creating trial")
                val trialExpiry = Timestamp(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                val newTrial = hashMapOf(
                    "deviceId" to deviceId,
                    "type" to "trial",
                    "expiresAt" to trialExpiry,
                    "telegramId" to "",
                    "code" to ""
                )
                db.collection("subscriptions").document(deviceId).set(newTrial).await()
                Log.d(TAG, "Trial created successfully")
                return SubscriptionInfo("trial", trialExpiry.toDate().time, true)
            }

            val type = doc.getString("type") ?: "trial"
            val expiresAt = doc.getTimestamp("expiresAt")?.toDate()?.time ?: 0L
            val isActive = expiresAt > System.currentTimeMillis()

            Log.d(TAG, "Subscription found: type=$type isActive=$isActive")
            SubscriptionInfo(type, expiresAt, isActive)

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
            val codeDoc = db.collection("subscription_codes").document(code).get().await()

            if (!codeDoc.exists()) {
                Log.e(TAG, "Code not found: $code")
                return ActivationResult.CodeNotFound
            }

            val used = codeDoc.getBoolean("used") ?: true
            if (used) {
                Log.e(TAG, "Code already used: $code")
                return ActivationResult.CodeAlreadyUsed
            }

            val durationDays = codeDoc.getLong("durationDays") ?: 0L
            val newExpiry = Timestamp(Date(System.currentTimeMillis() + durationDays * 24 * 60 * 60 * 1000))

            val subscriptionUpdate = hashMapOf(
                "deviceId" to deviceId,
                "type" to "paid",
                "expiresAt" to newExpiry,
                "telegramId" to telegramId,
                "code" to code
            )
            db.collection("subscriptions").document(deviceId).set(subscriptionUpdate).await()

            db.collection("subscription_codes").document(code)
                .update("used", true, "usedBy", telegramId).await()

            Log.d(TAG, "Code activated successfully")
            ActivationResult.Success

        } catch (e: Exception) {
            val detail = describeError(e)
            Log.e(TAG, "activateCode failed: $detail", e)
            ActivationResult.Error(detail)
        }
    }
}
