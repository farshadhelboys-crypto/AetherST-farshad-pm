package io.github.immaghzbad.aetherst.platform

private object DesktopLicenseGate : LicenseGate {
    override suspend fun isConnectionAllowed(): Boolean = true
}

actual fun getLicenseGate(context: PlatformContext): LicenseGate = DesktopLicenseGate
