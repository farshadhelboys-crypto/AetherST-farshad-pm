package io.github.immaghzbad.aetherst.platform

expect class LicenseGate(context: PlatformContext) {
    suspend fun isConnectionAllowed(): Boolean
    fun startMonitoring(onExpired: () -> Unit)
    fun stopMonitoring()
}
