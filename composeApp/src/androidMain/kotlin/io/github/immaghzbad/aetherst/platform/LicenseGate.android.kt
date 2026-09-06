package io.github.immaghzbad.aetherst.platform

import io.github.immaghzbad.aetherst.subscription.SubscriptionRepository

private class AndroidLicenseGate(context: PlatformContext) : LicenseGate {
    private val repository = SubscriptionRepository(context.context.applicationContext)

    override suspend fun isConnectionAllowed(): Boolean {
        return runCatching {
            repository.forceRefreshStatus().isActive
        }.getOrElse {
            // If the repository cannot reach the server it may still return a
            // recently cached valid subscription according to its current policy.
            runCatching { repository.getSubscriptionStatus().isActive }.getOrDefault(false)
        }
    }
}

actual fun getLicenseGate(context: PlatformContext): LicenseGate = AndroidLicenseGate(context)
