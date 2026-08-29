package io.github.immaghzbad.aetherst.shared.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val ElegantPrimary = Color(0xFFD4AF37)
val ElegantOnPrimary = Color(0xFF000000)
val ElegantPrimaryContainer = Color(0xFF3A3120)
val ElegantOnPrimaryContainer = Color(0xFFF0D77B)
val ElegantSecondary = Color(0xFF9A9A94)
val ElegantBackground = Color(0xFF000000)
val ElegantSurface = Color(0xFF000000)
val ElegantSurfaceCard = Color(0xFF141414)
val ElegantSurfaceActive = Color(0xFF2A2A2A)
val ElegantOutline = Color(0xFF2A2A2A)
val ElegantTextPrimary = Color(0xFFF5F5F0)
val ElegantTextSecondary = Color(0xFF9A9A94)
val ConnectedGreen = Color(0xFF81C784)
val ScanningAmber = Color(0xFFFFB74D)
val ErrorRed = Color(0xFFF2B8B5)

object AppPalette {
    val accent = Color(0xFFD4AF37)
    val accentVariant = Color(0xFFB8860B)
    val accentVariantAlt = Color(0xFFF0D77B)
    val onAccent = Color(0xFF000000)

    val statusConnected = Color(0xFF34C759)
    val statusScanning = Color(0xFFFF9500)
    val statusError = Color(0xFFFF3B30)
    val debugCyan = Color(0xFF64D2FF)

    val surfaceRaised = Color(0xFF141414)
    val surfaceSunken = Color(0xFF000000)
    val groupBg = Color(0xFF1A1A1A)
    val divider = Color(0xFF2A2A2A)
    val inactiveTrack = Color(0xFF333333)

    val textPrimary = Color(0xFFF5F5F0)
    val textSecondary = Color(0xFF9A9A94)

    val navBackground = Color(0xFF141414)
    val navActive = Color(0xFFD4AF37)
    val navInactive = Color(0xFF6B6B66)
}

@Immutable
data class AppColors(
    val accent: Color,
    val onAccent: Color,
    val accentVariant: Color,
    val debugCyan: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val groupBg: Color,
    val divider: Color,
    val inactiveTrack: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val statusConnected: Color,
    val statusScanning: Color,
    val statusError: Color,
    val navBackground: Color,
    val navActive: Color,
    val navInactive: Color,
)

internal val darkAppColors = AppColors(
    accent = AppPalette.accent,
    onAccent = AppPalette.onAccent,
    accentVariant = AppPalette.accentVariant,
    debugCyan = AppPalette.debugCyan,
    surfaceRaised = AppPalette.surfaceRaised,
    surfaceSunken = AppPalette.surfaceSunken,
    groupBg = AppPalette.groupBg,
    divider = AppPalette.divider,
    inactiveTrack = AppPalette.inactiveTrack,
    textPrimary = AppPalette.textPrimary,
    textSecondary = AppPalette.textSecondary,
    statusConnected = AppPalette.statusConnected,
    statusScanning = AppPalette.statusScanning,
    statusError = AppPalette.statusError,
    navBackground = AppPalette.navBackground,
    navActive = AppPalette.navActive,
    navInactive = AppPalette.navInactive,
)

internal val lightAppColors = AppColors(
    accent = Color(0xFFB8860B),
    onAccent = Color(0xFFFFFFFF),
    accentVariant = Color(0xFF8C6A0E),
    debugCyan = Color(0xFF0A84FF),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFF7F6F2),
    groupBg = Color(0xFFEFEDE6),
    divider = Color(0xFFE0DED5),
    inactiveTrack = Color(0xFFD1CFC6),
    textPrimary = Color(0xFF1A1A17),
    textSecondary = Color(0xFF6B6B63),
    statusConnected = Color(0xFF1E9E6B),
    statusScanning = Color(0xFFC77D1E),
    statusError = Color(0xFFC0392B),
    navBackground = Color(0xFFF7F6F2),
    navActive = Color(0xFFB8860B),
    navInactive = Color(0xFF8A8A80),
)

val LocalAppColors = staticCompositionLocalOf { darkAppColors }

@Composable
fun appColors(): AppColors = LocalAppColors.current
