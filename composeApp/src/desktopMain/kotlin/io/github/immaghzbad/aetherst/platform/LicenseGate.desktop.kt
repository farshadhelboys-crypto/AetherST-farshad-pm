package io.github.immaghzbad.aetherst.platform

actual class LicenseGate actual constructor(context: PlatformContext) {
    actual suspend fun isConnectionAllowed(): Boolean = true
    actual fun startMonitoring(onExpired: () -> Unit) {}
    actual fun stopMonitoring() {}
}
