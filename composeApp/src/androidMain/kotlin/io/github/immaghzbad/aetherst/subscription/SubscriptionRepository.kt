package io.github.immaghzbad.aetherst.subscription

import android.content.Context
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.net.InetSocketAddress
import java.net.Proxy

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

    private val db: FirebaseFirestore

    init {
        // ============================================
        // تنظیم پروکسی FOD (رایگان و مخصوص ایران)
        // ============================================
        try {
            // تنظیم پروکسی برای HTTP/HTTPS
            System.setProperty("https.proxyHost", "fod.backtory.com")
            System.setProperty("https.proxyPort", "8118")
            System.setProperty("http.proxyHost", "fod.backtory.com")
            System.setProperty("http.proxyPort", "8118")
            
            // تنظیم پروکسی برای gRPC (مخصوص Firestore)
            System.setProperty("grpc.proxy", "http://fod.backtory.com:8118")
            
            // تنظیمات Firestore
            val settings = FirebaseFirestoreSettings.Builder()
                .setHost("firestore.googleapis.com")
                .setUseExperimentalThreadPool(true)
                .build()
            
            db = FirebaseFirestore.getInstance()
            db.firestoreSettings = settings
            
            android.util.Log.d("SubscriptionRepo", "✅ پروکسی FOD با موفقیت تنظیم شد")
        } catch (e: Exception) {
            android.util.Log.e("SubscriptionRepo", "❌ خطا در تنظیم پروکسی: ${e.message}")
            // اگه پروکسی تنظیم نشد، همچنان Firestore رو مقداردهی کن
            db = FirebaseFirestore.getInstance()
        }
    }

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    suspend fun getSubscriptionStatus(): SubscriptionInfo {
        val deviceId = getDeviceId()
        
        return try {
            android.util.Log.d("SubscriptionRepo", "📡 در حال دریافت وضعیت اشتراک برای دستگاه: $deviceId")
            
            val doc = db.collection("subscriptions").document(deviceId).get().await()

            if (!doc.exists()) {
                android.util.Log.d("SubscriptionRepo", "🆕 دستگاه جدید، ایجاد Trial")
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

            android.util.Log.d("SubscriptionRepo", "✅ وضعیت اشتراک: $type - فعال: $isActive")
            SubscriptionInfo(type, expiresAt, isActive)
            
        } catch (e: Exception) {
            android.util.Log.e("SubscriptionRepo", "❌ خطا در دریافت اشتراک: ${e.message}")
            // در صورت خطا، یک وضعیت پیش‌فرض (Trial 24 ساعته) برگردان
            SubscriptionInfo("trial", System.currentTimeMillis() + 24 * 60 * 60 * 1000, true)
        }
    }

    suspend fun activateCode(code: String, telegramId: String): ActivationResult {
        return try {
            android.util.Log.d("SubscriptionRepo", "🔑 تلاش برای فعال‌سازی کد: $code")
            
            val deviceId = getDeviceId()
            val codeDoc = db.collection("subscription_codes").document(code).get().await()

            if (!codeDoc.exists()) {
                android.util.Log.e("SubscriptionRepo", "❌ کد پیدا نشد")
                return ActivationResult.CodeNotFound
            }

            val used = codeDoc.getBoolean("used") ?: true
            if (used) {
                android.util.Log.e("SubscriptionRepo", "❌ کد قبلاً استفاده شده")
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

            android.util.Log.d("SubscriptionRepo", "✅ کد با موفقیت فعال شد")
            ActivationResult.Success
            
        } catch (e: Exception) {
            android.util.Log.e("SubscriptionRepo", "❌ خطا در فعال‌سازی کد: ${e.message}")
            ActivationResult.Error(e.message ?: "Unknown error")
        }
    }
}
