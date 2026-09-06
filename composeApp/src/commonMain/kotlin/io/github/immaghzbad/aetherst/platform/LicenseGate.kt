package io.github.immaghzbad.aetherst.platform

/**
 * Platform abstraction for the subscription gate.
 * Android performs the real subscription check; desktop keeps the app unrestricted.
 */
interface LicenseGate {
    suspend fun isConnectionAllowed(): Boolean
}

expect fun getLicenseGate(context: PlatformContext): LicenseGate
