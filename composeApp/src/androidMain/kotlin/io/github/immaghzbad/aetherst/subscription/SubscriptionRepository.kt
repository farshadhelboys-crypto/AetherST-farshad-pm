package io.github.immaghzbad.aetherst.subscription

import android.content.Context
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.Date

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

    private val db = FirebaseFirestore.getInstance()

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    suspend fun getSubscriptionStatus(): SubscriptionInfo {
        val deviceId = getDeviceId()
        val doc = db.collection("subscriptions").document(deviceId).get().await()

        if (!doc.exists()) {
            val trialExpiry = Timestamp(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
            val newTrial = hashMapOf(
                "deviceId" to deviceId,
                "type" to "trial",
                "expiresAt" to trialExpiry,
                "telegramId" to "",
                "code" to ""
            )
            db.collection("subscriptions").document(deviceId).set(newTrial).await()
            return SubscriptionInfo("trial", trialExpiry.toDate().time, true)
        }

        val type = doc.getString("type") ?: "trial"
        val expiresAt = doc.getTimestamp("expiresAt")?.toDate()?.time ?: 0L
        val isActive = expiresAt > System.currentTimeMillis()

        return SubscriptionInfo(type, expiresAt, isActive)
    }

    suspend fun activateCode(code: String, telegramId: String): ActivationResult {
        val deviceId = getDeviceId()
        val codeDoc = db.collection("subscription_codes").document(code).get().await()

        if (!codeDoc.exists()) {
            return ActivationResult.CodeNotFound
        }

        val used = codeDoc.getBoolean("used") ?: true
        if (used) {
            return ActivationResult.CodeAlreadyUsed
        }

        val durationDays = codeDoc.getLong("durationDays") ?: 0L
        val newExpiry = Timestamp(Date(System.currentTimeMillis() + durationDays * 24 * 60 * 60 * 1000))

        return try {
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

            ActivationResult.Success
        } catch (e: Exception) {
            ActivationResult.Error(e.message ?: "Unknown error")
        }
    }
}
