package io.github.immaghzbad.aetherst.subscription

import android.content.Context
import android.provider.Settings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class SubscriptionInfo(
    val type: String,
    val expiresAtMillis: Long,
    val isActive: Boolean
)

sealed class ActivationResult {
    object Success : ActivationResult()
    object CodeNotFound : ActivationResult()
    object CodeAlreadyUsed : ActivationResult()
    object DeviceMismatch : ActivationResult()
    data class Error(val message: String) : ActivationResult()
}

@Serializable
data class LicenseData(
    val code: String,
    val deviceId: String,
    val expiresAt: Long,
    val isUsed: Boolean = false,
    val usedByDevice: String? = null,
    val durationDays: Long = 30,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class DeviceData(
    val deviceId: String,
    val licenseCode: String? = null,
    val expiresAt: Long = 0,
    val type: String = "trial"
)

class SubscriptionRepository(private val context: Context) {

    // GitHub Repository Configuration
    // توجه: این مقادیر را با اطلاعات مخزن خود جایگزین کنید
    private val GITHUB_OWNER = "YOUR_USERNAME"
    private val GITHUB_REPO = "YOUR_REPO_NAME"
    private val GITHUB_BRANCH = "main"
    private val GITHUB_TOKEN = "YOUR_GITHUB_TOKEN" // با توکن خود جایگزین کنید
    
    // مسیرهای فایل‌ها در مخزن
    private val LICENSES_PATH = "licenses"
    private val DEVICES_PATH = "devices"
    
    private val client = HttpClient(CIO)
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val baseUrl = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/contents"

    fun getDeviceId(): String {
        val id = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        } ?: run {
            // برای ویندوز و fallback
            java.util.UUID.randomUUID().toString()
        }
        return id
    }

    suspend fun getSubscriptionStatus(): SubscriptionInfo = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId()
            val deviceFileUrl = "$baseUrl/$DEVICES_PATH/$deviceId.json"
            
            val response = try {
                client.get(deviceFileUrl) {
                    header("Authorization", "token $GITHUB_TOKEN")
                    header("Accept", "application/vnd.github.v3+json")
                }
            } catch (e: Exception) {
                // فایل دستگاه وجود ندارد - ایجاد trial جدید
                return@withContext createNewTrial(deviceId)
            }
            
            if (response.status.value != 200) {
                return@withContext createNewTrial(deviceId)
            }
            
            val responseBody = response.body<String>()
            // استخراج محتوای base64 از پاسخ GitHub
            val content = json.decodeFromString<GitHubFileResponse>(responseBody)
            val decodedContent = String(android.util.Base64.decode(content.content, android.util.Base64.DEFAULT))
            
            val deviceData = json.decodeFromString<DeviceData>(decodedContent)
            
            val isActive = deviceData.expiresAt > System.currentTimeMillis()
            
            return@withContext SubscriptionInfo(
                type = deviceData.type,
                expiresAtMillis = deviceData.expiresAt,
                isActive = isActive
            )
            
        } catch (e: Exception) {
            // در صورت خطا، trial موقت ایجاد کن
            val deviceId = getDeviceId()
            return@withContext createNewTrial(deviceId)
        }
    }

    private suspend fun createNewTrial(deviceId: String): SubscriptionInfo {
        val trialExpiry = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 24 ساعت
        val deviceData = DeviceData(
            deviceId = deviceId,
            type = "trial",
            expiresAt = trialExpiry
        )
        
        try {
            // آپلود فایل دستگاه به GitHub
            val content = json.encodeToString(DeviceData.serializer(), deviceData)
            val encodedContent = android.util.Base64.encodeToString(content.toByteArray(), android.util.Base64.NO_WRAP)
            
            val commitData = mapOf(
                "message" to "Create trial for device $deviceId",
                "content" to encodedContent,
                "branch" to GITHUB_BRANCH
            )
            
            client.post("$baseUrl/$DEVICES_PATH/$deviceId.json") {
                contentType(ContentType.Application.Json)
                header("Authorization", "token $GITHUB_TOKEN")
                header("Accept", "application/vnd.github.v3+json")
                setBody(json.encodeToString(commitData))
            }
        } catch (e: Exception) {
            // خطا در آپلود - اما همچنان trial را برمی‌گردانیم
        }
        
        return SubscriptionInfo("trial", trialExpiry, true)
    }

    suspend fun activateCode(code: String, telegramId: String): ActivationResult = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId()
            
            // ۱. بررسی وجود لایسنس
            val licenseUrl = "$baseUrl/$LICENSES_PATH/$code.json"
            val licenseResponse = try {
                client.get(licenseUrl) {
                    header("Authorization", "token $GITHUB_TOKEN")
                    header("Accept", "application/vnd.github.v3+json")
                }
            } catch (e: Exception) {
                return@withContext ActivationResult.CodeNotFound
            }
            
            if (licenseResponse.status.value != 200) {
                return@withContext ActivationResult.CodeNotFound
            }
            
            val responseBody = licenseResponse.body<String>()
            val content = json.decodeFromString<GitHubFileResponse>(responseBody)
            val decodedContent = String(android.util.Base64.decode(content.content, android.util.Base64.DEFAULT))
            val licenseData = json.decodeFromString<LicenseData>(decodedContent)
            
            // ۲. بررسی استفاده شده بودن
            if (licenseData.isUsed) {
                // اگر DeviceMismatch باشد، اجازه استفاده مجدد نمی‌دهیم
                if (licenseData.usedByDevice != null && licenseData.usedByDevice != deviceId) {
                    return@withContext ActivationResult.DeviceMismatch
                }
                return@withContext ActivationResult.CodeAlreadyUsed
            }
            
            // ۳. محاسبه زمان انقضا
            val newExpiry = System.currentTimeMillis() + licenseData.durationDays * 24 * 60 * 60 * 1000
            
            // ۴. آپدیت دستگاه
            val deviceData = DeviceData(
                deviceId = deviceId,
                licenseCode = code,
                expiresAt = newExpiry,
                type = "paid"
            )
            
            val deviceContent = json.encodeToString(DeviceData.serializer(), deviceData)
            val encodedDeviceContent = android.util.Base64.encodeToString(deviceContent.toByteArray(), android.util.Base64.NO_WRAP)
            
            val deviceCommitData = mapOf(
                "message" to "Activate license $code for device $deviceId",
                "content" to encodedDeviceContent,
                "branch" to GITHUB_BRANCH
            )
            
            val devicePutResponse = client.post("$baseUrl/$DEVICES_PATH/$deviceId.json") {
                contentType(ContentType.Application.Json)
                header("Authorization", "token $GITHUB_TOKEN")
                header("Accept", "application/vnd.github.v3+json")
                setBody(json.encodeToString(deviceCommitData))
            }
            
            // ۵. آپدیت لایسنس (علامت‌گذاری به عنوان استفاده شده)
            val updatedLicense = licenseData.copy(
                isUsed = true,
                usedByDevice = deviceId
            )
            
            val licenseContent = json.encodeToString(LicenseData.serializer(), updatedLicense)
            val encodedLicenseContent = android.util.Base64.encodeToString(licenseContent.toByteArray(), android.util.Base64.NO_WRAP)
            
            val licenseCommitData = mapOf(
                "message" to "Mark license $code as used by $deviceId",
                "content" to encodedLicenseContent,
                "branch" to GITHUB_BRANCH,
                "sha" to content.sha // برای آپدیت فایل موجود
            )
            
            val licensePutResponse = client.post("$baseUrl/$LICENSES_PATH/$code.json") {
                contentType(ContentType.Application.Json)
                header("Authorization", "token $GITHUB_TOKEN")
                header("Accept", "application/vnd.github.v3+json")
                setBody(json.encodeToString(licenseCommitData))
            }
            
            return@withContext ActivationResult.Success
            
        } catch (e: Exception) {
            return@withContext ActivationResult.Error(e.message ?: "Unknown error")
        }
    }
    
    // کلاس برای پاسخ GitHub API
    @Serializable
    private data class GitHubFileResponse(
        val content: String,
        val sha: String,
        val encoding: String = "base64"
    )
}
