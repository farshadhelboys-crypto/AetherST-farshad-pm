package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.immaghzbad.aetherst.platform.isDesktop
import io.github.immaghzbad.aetherst.shared.core.NetworkUtils
import io.github.immaghzbad.aetherst.shared.data.PsiphonEgressRegistry
import io.github.immaghzbad.aetherst.shared.model.AetherConfig
import io.github.immaghzbad.aetherst.shared.model.AetherIpMode
import io.github.immaghzbad.aetherst.shared.model.AetherLogLevel
import io.github.immaghzbad.aetherst.shared.model.AetherNoise
import io.github.immaghzbad.aetherst.shared.model.AetherPerfProfile
import io.github.immaghzbad.aetherst.shared.model.AetherProtocol
import io.github.immaghzbad.aetherst.shared.model.AetherScanMode
import io.github.immaghzbad.aetherst.shared.model.ConnectionMode
import io.github.immaghzbad.aetherst.shared.model.TunnelEngine
import io.github.immaghzbad.aetherst.shared.ui.components.AppDivider
import io.github.immaghzbad.aetherst.shared.ui.components.IosActionRow
import io.github.immaghzbad.aetherst.shared.ui.components.IosConfirmationDialog
import io.github.immaghzbad.aetherst.shared.ui.components.IosGroupCard
import io.github.immaghzbad.aetherst.shared.ui.components.IosIconBadge
import io.github.immaghzbad.aetherst.shared.ui.components.IosInputField
import io.github.immaghzbad.aetherst.shared.ui.components.IosInputFieldRow
import io.github.immaghzbad.aetherst.shared.ui.components.IosPickerRow
import io.github.immaghzbad.aetherst.shared.ui.components.IosPresetItem
import io.github.immaghzbad.aetherst.shared.ui.components.IosSwitchRow
import io.github.immaghzbad.aetherst.shared.ui.theme.AppPalette
import io.github.immaghzbad.aetherst.shared.util.CountryNames

private val IosCardBg = AppPalette.surfaceRaised
private val IosGroupBg = AppPalette.divider
private val IosSecondaryLabel = AppPalette.textSecondary
private val IosActiveBlue = AppPalette.accent
private val IosDividerColor = AppPalette.divider
private val IosActiveGreen = AppPalette.statusConnected

enum class SettingsPage(val title: String) {
    PRESETS("پروفایل‌های تنظیمات"),
    CONNECTION("اتصال و تونل‌زنی"),
    PROTOCOL("پروتکل و حمل‌ونقل"),
    ZEROTRUST("Cloudflare Zero Trust"),
    NETWORK("پارامترهای شبکه"),
    SECURITY("امنیت و قابلیت اطمینان"),
    DIAGNOSTICS("تشخیص و هسته"),
    PSIPHON("زنجیره سایفون"),
    SYSTEM("سیستم و نگهداری"),
    HEV_ENGINE("موتور HEV")
}

@Composable
fun SettingsScreen(
    config: AetherConfig,
    isBatteryOptimized: Boolean,
    onUpdateConfig: (AetherConfig) -> Unit,
    onUpdateTunnelEngine: (TunnelEngine) -> Unit,
    onApplyPreset: (String) -> Unit,
    onOpenSplitTunneling: () -> Unit,
    onOpenRoutingRules: () -> Unit,
    onOpenAutoDetect: () -> Unit = {},
    onOpenSpeedTest: () -> Unit = {},
    onRequestBatteryOptimization: () -> Unit,
    onOpenVpnSettings: () -> Unit = {},
    onResetAll: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOptimizeMtu: () -> Unit,
    isOptimizingMtu: Boolean = false,
    onShowToast: (String, Boolean) -> Unit = { _, _ -> },
    initialPage: SettingsPage? = null,
    onSubPageClosed: () -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
) {
    var currentPage by remember { mutableStateOf(initialPage) }

    if (currentPage != null) {
        SettingsSubPage(
            page = currentPage!!,
            config = config,
            isBatteryOptimized = isBatteryOptimized,
            onBack = {
                currentPage = null
                onSubPageClosed()
            },
            onUpdateConfig = onUpdateConfig,
            onUpdateTunnelEngine = onUpdateTunnelEngine,
            onApplyPreset = onApplyPreset,
            onOpenSplitTunneling = onOpenSplitTunneling,
            onOpenRoutingRules = onOpenRoutingRules,
            onRequestBatteryOptimization = onRequestBatteryOptimization,
            onOpenVpnSettings = onOpenVpnSettings,
            onResetAll = onResetAll,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup,
            onOptimizeMtu = onOptimizeMtu,
            isOptimizingMtu = isOptimizingMtu,
            onShowToast = onShowToast,
            bottomContentPadding = bottomContentPadding
        )
        return
    }

    val isAndroid = remember { try { Class.forName("android.os.Build"); true } catch(_: Throwable) { false } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomContentPadding + 12.dp, top = if (isDesktop) 12.dp else 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text("تنظیمات Feri Pm Tunnel", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 26.sp, lineHeight = 30.sp)
                Text("پیکربندی پروتکل‌های موتور، مبهم‌سازی و حمل‌ونقل", color = IosSecondaryLabel, fontSize = 12.sp)
            }
        }
        item {
            IosGroupCard {
                Column {
                    IosActionRow(icon = Icons.Default.Speed, iconBg = AppPalette.statusScanning, title = "تست سرعت اینترنت", subtitle = "اندازه‌گیری دانلود، آپلود، پینگ و لرزش", onClick = onOpenSpeedTest)
                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp)
                    IosActionRow(icon = Icons.Default.Radar, iconBg = AppPalette.accent, title = "تشخیص خودکار هوشمند", subtitle = "تشخیص بهترین پروتکل و تنظیمات برای شبکه شما", onClick = onOpenAutoDetect)
                }
            }
        }
        item { CategoryCard(icon = Icons.Default.Tune, iconBg = AppPalette.textSecondary, title = "پروفایل‌های تنظیمات", subtitle = "پیش‌تنظیمات و تنظیمات دستی", onClick = { currentPage = SettingsPage.PRESETS }) }
        item { CategoryCard(icon = Icons.Default.VpnLock, iconBg = AppPalette.statusConnected, title = "اتصال و تونل‌زنی", subtitle = "حالت، موتور، تونل‌زنی تقسیم‌شده، مسیریابی", onClick = { currentPage = SettingsPage.CONNECTION }) }
        if (isAndroid) {
            item { CategoryCard(icon = Icons.Default.Shield, iconBg = AppPalette.accentVariant, title = "زنجیره سایفون", subtitle = "مسیریابی از طریق سایفون برای آی‌پی غیرایرانی", onClick = { currentPage = SettingsPage.PSIPHON }) }
        }
        item { CategoryCard(icon = Icons.Default.Shield, iconBg = IosActiveBlue, title = "پروتکل و حمل‌ونقل", subtitle = "MASQUE، H2، ECH، مبهم‌سازی، MTU", onClick = { currentPage = SettingsPage.PROTOCOL }) }
        if (config.protocol == AetherProtocol.ZERO_TRUST) {
            item { CategoryCard(icon = Icons.Default.Business, iconBg = AppPalette.accentVariant, title = "Cloudflare Zero Trust", subtitle = "تیم، دروازه و احراز هویت", onClick = { currentPage = SettingsPage.ZEROTRUST }) }
        }
        item { CategoryCard(icon = Icons.Default.Language, iconBg = IosActiveBlue, title = "پارامترهای شبکه", subtitle = "SOCKS5، HTTP، پورت‌ها، DNS، همتا", onClick = { currentPage = SettingsPage.NETWORK }) }
        item { CategoryCard(icon = Icons.Default.Lock, iconBg = AppPalette.statusError, title = "امنیت و قابلیت اطمینان", subtitle = "قطع کننده اتصال، نشت IPv6، اتصال مجدد", onClick = { currentPage = SettingsPage.SECURITY }) }
        item { CategoryCard(icon = Icons.Default.BugReport, iconBg = AppPalette.debugCyan, title = "تشخیص و هسته", subtitle = "ثبت وقایع، کارایی، پروکسی بالادست", onClick = { currentPage = SettingsPage.DIAGNOSTICS }) }
        if (isAndroid) {
            item { CategoryCard(icon = Icons.Default.Memory, iconBg = AppPalette.accentVariantAlt, title = "موتور HEV", subtitle = "سطح گزارش، زمان‌بندی‌ها، محدودیت جلسات (پیشرفته)", onClick = { currentPage = SettingsPage.HEV_ENGINE }) }
        }
        item { CategoryCard(icon = Icons.Default.Settings, iconBg = AppPalette.textSecondary, title = "سیستم و نگهداری", subtitle = "پشتیبان‌گیری، بازیابی، بازنشانی", onClick = { currentPage = SettingsPage.SYSTEM }) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CategoryCard(icon: ImageVector, iconBg: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = IosCardBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IosIconBadge(icon = icon, backgroundColor = iconBg)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 15.sp); Text(subtitle, color = IosSecondaryLabel, fontSize = 12.sp) }
            Icon(Icons.Default.ChevronRight, null, tint = IosSecondaryLabel, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsSubPage(page: SettingsPage, config: AetherConfig, isBatteryOptimized: Boolean, onBack: () -> Unit, onUpdateConfig: (AetherConfig) -> Unit, onUpdateTunnelEngine: (TunnelEngine) -> Unit, onApplyPreset: (String) -> Unit, onOpenSplitTunneling: () -> Unit, onOpenRoutingRules: () -> Unit, onRequestBatteryOptimization: () -> Unit, onOpenVpnSettings: () -> Unit, onResetAll: () -> Unit, onExportBackup: () -> Unit, onImportBackup: () -> Unit, onOptimizeMtu: () -> Unit, isOptimizingMtu: Boolean, onShowToast: (String, Boolean) -> Unit, bottomContentPadding: Dp) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showAdvancedZt by remember { mutableStateOf(false) }
    val isAndroid = remember { try { Class.forName("android.os.Build"); true } catch(_: Throwable) { false } }
    val focusManager = LocalFocusManager.current

    io.github.immaghzbad.aetherst.shared.ui.components.PlatformBackHandler(enabled = true, onBack = onBack)

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { focusManager.clearFocus() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = if (isDesktop) 12.dp else 36.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
            Text(page.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomContentPadding + 12.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (page) {
                SettingsPage.PRESETS -> item { PresetPage(config, onApplyPreset, onShowToast) }
                SettingsPage.CONNECTION -> item { ConnectionPage(config, isAndroid, onUpdateConfig, onUpdateTunnelEngine, onOpenSplitTunneling, onOpenRoutingRules) }
                SettingsPage.PROTOCOL -> item { ProtocolPage(config, onUpdateConfig, onOptimizeMtu, isOptimizingMtu) }
                SettingsPage.ZEROTRUST -> item { ZeroTrustPage(config, showAdvancedZt, onUpdateConfig) { showAdvancedZt = it } }
                SettingsPage.NETWORK -> item { NetworkPage(config, isAndroid, onUpdateConfig) }
                SettingsPage.SECURITY -> item { SecurityPage(config, isAndroid, isBatteryOptimized, onUpdateConfig, onRequestBatteryOptimization) }
                SettingsPage.DIAGNOSTICS -> item { DiagnosticsPage(config, onUpdateConfig) }
                SettingsPage.PSIPHON -> if (isAndroid) item { PsiphonPage(config, onUpdateConfig) }
                SettingsPage.HEV_ENGINE -> item { HevEnginePage(config, onUpdateConfig) }
                SettingsPage.SYSTEM -> item { SystemPage(isAndroid, onExportBackup, onImportBackup, onOpenVpnSettings) { showResetDialog = true } }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
    if (showResetDialog) IosConfirmationDialog(
        title = "بازنشانی همه تنظیمات؟",
        message = "این کار همه پروتکل‌ها، تنظیمات موتور و تنظیمات امنیتی را به حالت پیش‌فرض کارخانه بازمی‌گرداند. این عمل قابل بازگشت نیست.",
        confirmText = "بازنشانی همه",
        confirmColor = AppPalette.statusError,
        onConfirm = { onResetAll(); showResetDialog = false; onShowToast("سیستم به حالت پیش‌فرض بازگردانده شد", false) },
        onDismiss = { showResetDialog = false }
    )
}

@Composable private fun PresetPage(config: AetherConfig, onApplyPreset: (String) -> Unit, onShowToast: (String, Boolean) -> Unit) {
    IosGroupCard { Column {
        IosPresetItem(icon = Icons.Default.Tune, iconBg = AppPalette.textSecondary, title = "تنظیمات دستی سفارشی", subtitle = "پیکربندی مستقل شما", isActive = config.presetId == "custom", onClick = { onApplyPreset("custom"); onShowToast("تنظیمات دستی اعمال شد", false) })
        AppDivider(); IosPresetItem(icon = Icons.Default.Lock, iconBg = AppPalette.accentVariant, title = "عبور از UDP / TLS", subtitle = "MASQUE + H2 Fallback + تکه‌تکه‌سازی", isActive = config.presetId == "bypass_udp", onClick = { onApplyPreset("bypass_udp"); onShowToast("پیش‌تنظیم عبور از UDP/TLS اعمال شد", false) })
        AppDivider(); IosPresetItem(icon = Icons.Default.Shield, iconBg = IosActiveBlue, title = "مخفی کاری آهنین", subtitle = "MASQUE + GFW Noise + Ironclad Probe", isActive = config.presetId == "ironclad_stealth", onClick = { onApplyPreset("ironclad_stealth"); onShowToast("پیش‌تنظیم مخفی کاری آهنین اعمال شد", false) })
        AppDivider(); IosPresetItem(icon = Icons.Default.Bolt, iconBg = AppPalette.statusScanning, title = "سرعت توربو", subtitle = "WireGuard + Balanced Noise + Turbo Scan", isActive = config.presetId == "turbo_wg", onClick = { onApplyPreset("turbo_wg"); onShowToast("پیش‌تنظیم سرعت توربو اعمال شد", false) })
    } }
}

@Composable private fun ConnectionPage(config: AetherConfig, isAndroid: Boolean, onUpdateConfig: (AetherConfig) -> Unit, onUpdateTunnelEngine: (TunnelEngine) -> Unit, onOpenSplitTunneling: () -> Unit, onOpenRoutingRules: () -> Unit) {
    IosGroupCard {
        IosPickerRow(
            icon = Icons.Default.TouchApp,
            iconBg = IosActiveBlue,
            title = "سبک دکمه اتصال",
            value = if (config.connectButtonStyle == "capsule") "لمسی (کپسولی)" else "کشویی",
            options = listOf("کشویی", "لمسی (کپسولی)"),
            onOptionSelected = { idx -> onUpdateConfig(config.copy(connectButtonStyle = if (idx == 0) "swipe" else "capsule")) }
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    IosGroupCard { Column {
        val opts = if (isAndroid) listOf("تونل", "فقط پروکسی") else if (isDesktop) listOf("حالت TUN (سراسری)", "پروکسی سیستم", "فقط پروکسی") else listOf("حالت TUN (سراسری)", "پروکسی سیستم", "فقط پروکسی")
        IosPickerRow(icon = Icons.Default.VpnLock, iconBg = AppPalette.statusConnected, title = "حالت اتصال", value = when (config.connectionMode) { ConnectionMode.TUNNEL -> if (isAndroid) "تونل" else "حالت TUN (سراسری)"; ConnectionMode.SYSTEM_PROXY -> "پروکسی سیستم"; else -> "فقط پروکسی" }, options = opts, onOptionSelected = { val m = if (isAndroid) { if (it == 0) ConnectionMode.TUNNEL else ConnectionMode.PROXY_ONLY } else if (isDesktop) { when (it) { 0 -> ConnectionMode.TUNNEL; 1 -> ConnectionMode.SYSTEM_PROXY; else -> ConnectionMode.PROXY_ONLY } } else { when (it) { 0 -> ConnectionMode.TUNNEL; 1 -> ConnectionMode.SYSTEM_PROXY; else -> ConnectionMode.PROXY_ONLY } }; onUpdateConfig(config.copy(connectionMode = m)) })
        if (config.connectionMode == ConnectionMode.TUNNEL) {
            AppDivider(); IosPickerRow(icon = Icons.Default.VpnLock, iconBg = AppPalette.accentVariant, title = "موتور تونل", value = config.tunnelEngine.displayName, options = TunnelEngine.entries.map { it.displayName }, onOptionSelected = { onUpdateTunnelEngine(TunnelEngine.entries[it]) })
            if (!isDesktop) {
                AppDivider(); IosSwitchRow(icon = Icons.Default.AllInclusive, iconBg = IosActiveBlue, title = "تونل کل دستگاه", subtitle = "مسیریابی همه ترافیک برنامه‌ها از طریق VPN", checked = config.tunnelAllApps, onCheckedChange = { onUpdateConfig(config.copy(tunnelAllApps = it)) }, testTag = "switch_tunnel_all"); AppDivider(); IosPickerRow(icon = Icons.Default.Tune, iconBg = AppPalette.accentVariant, title = "تونل‌زنی تقسیم‌شده", value = if (config.tunnelAllApps) "همه برنامه‌ها تونل می‌شوند" else "${config.tunneledPackages.size + config.blockedPackages.size} برنامه • پیش‌فرض عبور", options = emptyList(), onOptionSelected = {}, onClickOverride = onOpenSplitTunneling, enabled = !config.tunnelAllApps)
            }
            AppDivider()
        }
        IosPickerRow(icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = IosActiveBlue, title = "مسیریابی دامنه و آی‌پی", value = "${config.routingRules.size} قانون", options = emptyList(), onOptionSelected = {}, onClickOverride = onOpenRoutingRules)
        if (isAndroid) { AppDivider(); IosSwitchRow(icon = Icons.Default.Share, iconBg = AppPalette.accentVariantAlt, title = "اشتراک‌گذاری از طریق هات‌اسپات", subtitle = "اجازه اتصال دستگاه‌های دیگر به پروکسی", checked = config.shareHotspot, onCheckedChange = { onUpdateConfig(config.copy(shareHotspot = it)) }, testTag = "switch_share_hotspot"); if (config.shareHotspot) HotspotInfo(config) }
    } }
}

@Composable private fun HotspotInfo(config: AetherConfig) {
    Column(modifier = Modifier.fillMaxWidth().background(IosGroupBg.copy(alpha = 0.4f)).padding(14.dp)) {
        var localIp by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) { localIp = NetworkUtils.getLocalIpAddress() }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { Icon(if (localIp != null) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (localIp != null) IosActiveGreen else AppPalette.statusScanning, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(if (localIp != null) "هات‌اسپات فعال" else "هات‌اسپات غیرفعال", color = if (localIp != null) IosActiveGreen else AppPalette.statusScanning, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            IconButton(onClick = { localIp = NetworkUtils.getLocalIpAddress() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Refresh, null, tint = IosActiveBlue, modifier = Modifier.size(18.dp)) }
        }
        if (localIp != null) { Spacer(modifier = Modifier.height(10.dp)); Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color.Black.copy(alpha = 0.3f)) { Column(modifier = Modifier.padding(12.dp)) { Text("آدرس پروکسی", color = IosSecondaryLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp); Spacer(modifier = Modifier.height(6.dp)); Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("$localIp:${config.socksPort}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) } } } }
    }
}

@Composable private fun ProtocolPage(config: AetherConfig, onUpdateConfig: (AetherConfig) -> Unit, onOptimizeMtu: () -> Unit, isOptimizingMtu: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IosGroupCard { Column {
            IosPickerRow(icon = Icons.Default.VpnLock, iconBg = IosActiveBlue, title = "پروتکل حمل‌ونقل", value = config.protocol.displayName, options = AetherProtocol.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(protocol = AetherProtocol.entries[it])) })
            if (config.protocol == AetherProtocol.MASQUE) { AppDivider(); IosSwitchRow(icon = Icons.Default.Http, iconBg = IosActiveBlue, title = "حالت بازگشت HTTP/2", subtitle = "اجبار MASQUE از طریق TCP/TLS به جای QUIC", checked = config.h2Mode, onCheckedChange = { onUpdateConfig(config.copy(h2Mode = it)) }, testTag = "switch_h2_mode"); AppDivider(); IosSwitchRow(icon = Icons.Default.VerticalSplit, iconBg = AppPalette.accentVariant, title = "تکه‌تکه‌سازی بسته‌ها", subtitle = "عبور از فیلترهای SNI (فقط حالت H2)", checked = config.h2Fragment, onCheckedChange = { onUpdateConfig(config.copy(h2Fragment = it)) }, testTag = "switch_fragment"); if (config.h2Fragment) { IosInputFieldRow(icon = Icons.Default.Straighten, iconBg = IosSecondaryLabel, label = "اندازه تکه (بایت)", value = config.fragmentSize, onValueChange = { onUpdateConfig(config.copy(fragmentSize = it)) }, placeholder = "16-32", testTag = "fragment_size_input"); AppDivider(); IosInputFieldRow(icon = Icons.Default.Timer, iconBg = IosSecondaryLabel, label = "تاخیر تکه (میلی‌ثانیه)", value = config.fragmentDelay, onValueChange = { onUpdateConfig(config.copy(fragmentDelay = it)) }, placeholder = "2-10", testTag = "fragment_delay_input"); AppDivider() }; IosSwitchRow(icon = Icons.Default.EnhancedEncryption, iconBg = IosActiveGreen, title = "ECH رمزگذاری شده Client Hello", subtitle = "پنهان‌سازی SNI از ناظران شبکه", checked = config.echEnabled, onCheckedChange = { onUpdateConfig(config.copy(echEnabled = it)) }, testTag = "switch_ech_enabled"); AppDivider() }
            IosSwitchRow(icon = Icons.Default.DataUsage, iconBg = AppPalette.statusScanning, title = "غیرفعال‌سازی تأیید داده", subtitle = "صرف‌نظر از انتظار برای تبادل اولیه بسته", checked = config.noDataCheck, onCheckedChange = { onUpdateConfig(config.copy(noDataCheck = it)) }, testTag = "switch_no_data_check"); AppDivider()
            val availNoise = if (config.protocol == AetherProtocol.MASQUE) listOf(AetherNoise.FIREWALL, AetherNoise.GFW, AetherNoise.OFF) else listOf(AetherNoise.BALANCED, AetherNoise.AGGRESSIVE, AetherNoise.LIGHT, AetherNoise.OFF)
            IosPickerRow(icon = Icons.Default.Tune, iconBg = AppPalette.accentVariantAlt, title = "مبهم‌سازی عبور", value = config.noise.displayName.substringBefore(" ("), options = availNoise.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(noise = availNoise[it])) }); AppDivider()
            IosPickerRow(icon = Icons.Default.NetworkCheck, iconBg = AppPalette.statusScanning, title = "استراتژی سرعت", value = config.scanMode.name.lowercase().replaceFirstChar { it.uppercase() }, options = AetherScanMode.entries.map { "${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }} (${it.description})" }, onOptionSelected = { onUpdateConfig(config.copy(scanMode = AetherScanMode.entries[it])) }); AppDivider()
            IosPickerRow(icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = AppPalette.accentVariant, title = "پشته شبکه", value = config.ipMode.rawValue, options = AetherIpMode.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(ipMode = AetherIpMode.entries[it])) }); AppDivider()
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Bottom) { Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Tune, backgroundColor = IosActiveGreen); Spacer(modifier = Modifier.width(12.dp)); IosInputField(label = "اندازه MTU سفارشی", value = config.mtu.toString(), onValueChange = { onUpdateConfig(config.copy(mtu = it.toIntOrNull() ?: 1100)) }, modifier = Modifier.weight(1f), placeholder = "1100", keyboardType = KeyboardType.Number, testTag = "mtu_input") }; Spacer(modifier = Modifier.width(8.dp)); Button(onClick = onOptimizeMtu, enabled = !isOptimizingMtu, modifier = Modifier.height(46.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = IosActiveBlue.copy(alpha = 0.15f), contentColor = IosActiveBlue, disabledContainerColor = IosActiveBlue.copy(alpha = 0.05f), disabledContentColor = IosActiveBlue.copy(alpha = 0.3f)), contentPadding = PaddingValues(horizontal = 16.dp)) { if (isOptimizingMtu) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = IosActiveBlue, strokeWidth = 2.dp) else Text("بهینه‌سازی", fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
        } }
        Text("مبهم‌سازی CLOAK (بومی - فقط MASQUE H2)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = IosSecondaryLabel, fontSize = 11.sp, letterSpacing = 0.5.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
        IosGroupCard { Column {
            IosSwitchRow(icon = Icons.Default.Security, iconBg = AppPalette.accentVariant, title = "Cloak Decoy (بومی)", subtitle = if (config.cloakEnabled) "فعال - TTL فریبنده برای عبور از DPI" else "غیرفعال - کتابخانه بومی JNI libcloak.so", checked = config.cloakEnabled, onCheckedChange = { onUpdateConfig(config.copy(cloakEnabled = it)) }, testTag = "switch_cloak_enabled")
            if (config.cloakEnabled) {
                AppDivider()
                if (config.protocol != AetherProtocol.MASQUE || !config.h2Mode) {
                    Row(modifier = Modifier.fillMaxWidth().background(AppPalette.statusScanning.copy(alpha = 0.12f)).padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, tint = AppPalette.statusScanning, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cloak فقط با MASQUE + H2 Fallback کار می‌کند. فعلی: ${config.protocol.displayName} ${if (config.h2Mode) "H2" else "H3"}", color = AppPalette.statusScanning, fontSize = 11.sp, lineHeight = 14.sp)
                    }
                    AppDivider()
                }
                IosInputFieldRow(icon = Icons.Default.Public, iconBg = IosActiveGreen, label = "لیست SNI فریبنده", value = config.cloakSniList, onValueChange = { onUpdateConfig(config.copy(cloakSniList = it)) }, placeholder = "www.bing.com,www.hcaptcha.com", testTag = "cloak_sni_input"); AppDivider()
                IosInputFieldRow(icon = Icons.Default.Timer, iconBg = AppPalette.statusScanning, label = "لیست TTL", value = config.cloakTtlList, onValueChange = { onUpdateConfig(config.copy(cloakTtlList = it)) }, placeholder = "4,5,6,8", testTag = "cloak_ttl_input"); AppDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Timer, backgroundColor = IosSecondaryLabel); Spacer(modifier = Modifier.width(12.dp)); IosInputField(label = "حداقل Jitter (ms)", value = config.cloakJitterMin.toString(), onValueChange = { onUpdateConfig(config.copy(cloakJitterMin = it.toIntOrNull() ?: 20)) }, placeholder = "20", keyboardType = KeyboardType.Number, testTag = "cloak_jitter_min") }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosInputField(label = "حداکثر Jitter (ms)", value = config.cloakJitterMax.toString(), onValueChange = { onUpdateConfig(config.copy(cloakJitterMax = it.toIntOrNull() ?: 80)) }, placeholder = "80", keyboardType = KeyboardType.Number, testTag = "cloak_jitter_max") }
                }; AppDivider()
                IosSwitchRow(icon = Icons.Default.VerticalSplit, iconBg = AppPalette.accentVariant, title = "تکه‌تکه‌سازی Hello واقعی", subtitle = "تقسیم SNI در بین بسته‌ها (سبک PattNG)", checked = config.cloakFragment, onCheckedChange = { onUpdateConfig(config.copy(cloakFragment = it)) }, testTag = "switch_cloak_fragment"); AppDivider()
                IosSwitchRow(icon = Icons.Default.Refresh, iconBg = IosActiveGreen, title = "آمار تطبیقی", subtitle = "به خاطر سپاری بهترین TTL/سرور از طریق cloak.stats", checked = config.cloakAdaptive, onCheckedChange = { onUpdateConfig(config.copy(cloakAdaptive = it)) }, testTag = "switch_cloak_adaptive"); AppDivider()
                IosSwitchRow(icon = Icons.Default.FontDownload, iconBg = IosSecondaryLabel, title = "تصادفی‌سازی حروف SNI", subtitle = "wWw.BiNg.CoM برای دور زدن تطابق دقیق", checked = config.cloakRandomizeSniCase, onCheckedChange = { onUpdateConfig(config.copy(cloakRandomizeSniCase = it)) }, testTag = "switch_cloak_randomize"); AppDivider()
                IosInputFieldRow(icon = Icons.Default.Dns, iconBg = IosActiveBlue, label = "پورت‌های بازگشت", value = config.cloakFallbackPorts, onValueChange = { onUpdateConfig(config.copy(cloakFallbackPorts = it)) }, placeholder = "443,2053,2083,2087,2096,8443", testTag = "cloak_fallback_input"); AppDivider()
                IosPickerRow(icon = Icons.Default.BugReport, iconBg = IosSecondaryLabel, title = "سطح گزارش Cloak", value = config.cloakLogLevel, options = listOf("error", "warn", "info", "debug"), onOptionSelected = { idx -> val lvl = listOf("error", "warn", "info", "debug")[idx]; onUpdateConfig(config.copy(cloakLogLevel = lvl)) })
            }
        } }
        IosGroupCard {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("درباره Cloak", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Cloak یک سیستم فریبنده بومی برای MASQUE از طریق H2 (TCP/TLS) است. قبل از دست دادن واقعی TLS، چند ClientHello فریبنده با TTL پایین و SNIهای رایج - برای مثال www.bing.com - ارسال می‌کند که در مسیر منقضی می‌شوند و توسط DPI دیده می‌شوند، و سپس دست دادن واقعی با SNI اصلی انجام می‌شود. این کار SNI واقعی را از فیلترهای مبتنی بر SNI پنهان می‌کند. فقط با MASQUE + H2 Fallback از طریق TCP کار می‌کند و زمانی مفید است که MASQUE مستقیم به دلیل بازرسی SNI شکست می‌خورد در حالی که مسیر شبکه زیرین همچنان قابل دسترسی است. وقتی MASQUE مستقیم وصل می‌شود، Cloak فقط بسته‌ها و Jitter اضافی اضافه می‌کند و هیچ مزیت سرعتی ندارد. گزارش‌ها در صفحه گزارش‌ها با برچسب CloakCore ظاهر می‌شوند.", color = IosSecondaryLabel, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable private fun ZeroTrustPage(config: AetherConfig, showAdvanced: Boolean, onUpdateConfig: (AetherConfig) -> Unit, onToggleAdvanced: (Boolean) -> Unit) {
    val isZt = config.protocol == AetherProtocol.ZERO_TRUST
    val ztError = if (isZt) config.zeroTrustError() else null
    val hasAuth = config.teamName.isNotBlank() &&
        (config.accessEmail.isNotBlank() || config.accessId.isNotBlank() || config.accessSecret.isNotBlank() || config.accessToken.isNotBlank())
    IosGroupCard { Column {
        IosInputFieldRow(icon = Icons.Default.Business, iconBg = AppPalette.accentVariant, label = if (isZt) "نام تیم سازمان *" else "نام تیم سازمان", value = config.teamName, onValueChange = { onUpdateConfig(config.copy(teamName = it)) }, placeholder = "مثلاً my-org", testTag = "zt_team_input"); AppDivider()
        IosInputFieldRow(icon = Icons.Default.Language, iconBg = IosActiveBlue, label = if (isZt) "ایمیل Cloudflare Access" else "ایمیل Cloudflare Access", value = config.accessEmail, onValueChange = { onUpdateConfig(config.copy(accessEmail = it)) }, placeholder = "user@example.com", testTag = "zt_email_input"); AppDivider()
        IosSwitchRow(icon = Icons.Default.Shield, iconBg = IosActiveGreen, title = "پروکسی فیلترینگ دروازه", subtitle = "مسیریابی از طریق دروازه سازمان برای فیلترینگ و گزارش‌ها (پیش‌فرض غیرفعال)", checked = config.useGateway, onCheckedChange = { onUpdateConfig(config.copy(useGateway = it)) }, testTag = "switch_zt_gateway"); AppDivider()
        IosSwitchRow(icon = Icons.Default.CheckCircle, iconBg = IosActiveBlue, title = "وارد شده بمانید", subtitle = "این دستگاه را ثبت‌شده نگه دارید (استفاده مجدد از توکن تا زمان انقضا)", checked = config.ztStaySignedIn, onCheckedChange = { onUpdateConfig(config.copy(ztStaySignedIn = it)) }, testTag = "switch_zt_stay_signed_in"); AppDivider()
        if (hasAuth) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onUpdateConfig(config.copy(teamName = "", accessEmail = "", accessId = "", accessSecret = "", accessToken = "", ztTokenExpiry = 0, ztStaySignedIn = false)) }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.AutoMirrored.Filled.Logout, backgroundColor = AppPalette.statusError); Spacer(modifier = Modifier.width(12.dp)); Text("خروج", fontWeight = FontWeight.Medium, color = AppPalette.statusError, fontSize = 15.sp) } }
        }
        Row(modifier = Modifier.fillMaxWidth().clickable { onToggleAdvanced(!showAdvanced) }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Lock, backgroundColor = IosSecondaryLabel); Spacer(modifier = Modifier.width(12.dp)); Text("احراز هویت پیشرفته", fontWeight = FontWeight.Medium, color = Color.White, fontSize = 15.sp) }; Icon(if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = IosSecondaryLabel, modifier = Modifier.size(18.dp)) }
        AnimatedVisibility(visible = showAdvanced, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) { Column(modifier = Modifier.fillMaxWidth().background(IosGroupBg.copy(alpha = 0.4f)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("دقیقاً یک روش ورود را انتخاب کنید.", color = IosSecondaryLabel, fontSize = 12.sp); IosInputField(label = if (isZt) "شناسه مشتری Access" else "شناسه مشتری Access", value = config.accessId, onValueChange = { onUpdateConfig(config.copy(accessId = it)) }, placeholder = "برای توکن‌های سرویس الزامی است", testTag = "zt_access_id"); IosInputField(label = if (isZt) "رمز مشتری Access" else "رمز مشتری Access", value = config.accessSecret, onValueChange = { onUpdateConfig(config.copy(accessSecret = it)) }, placeholder = "برای توکن‌های سرویس الزامی است", testTag = "zt_access_secret"); IosInputField(label = if (isZt) "توکن دسترسی JWT دستی" else "توکن دسترسی JWT دستی", value = config.accessToken, onValueChange = { onUpdateConfig(config.copy(accessToken = it, ztTokenExpiry = config.parseJwtExpiry(it))) }, placeholder = "توکن موجود که از قبل دارید", testTag = "zt_access_token") } }
    } }
    if (ztError != null) {
        Text(
            text = ztError,
            color = AppPalette.statusError,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
        )
    }
}

@Composable private fun NetworkPage(config: AetherConfig, isAndroid: Boolean, onUpdateConfig: (AetherConfig) -> Unit) {
    IosGroupCard { Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Language, backgroundColor = IosActiveBlue); Spacer(modifier = Modifier.width(12.dp)); IosInputField(label = "میزبان SOCKS5", value = config.socksHost, onValueChange = { onUpdateConfig(config.copy(socksHost = it)) }, modifier = Modifier.weight(1f), placeholder = "127.0.0.1", testTag = "socks_host_input"); Spacer(modifier = Modifier.width(10.dp)); IosInputField(label = "پورت SOCKS", value = config.socksPort, onValueChange = { onUpdateConfig(config.copy(socksPort = it)) }, modifier = Modifier.width(75.dp), placeholder = "1819", keyboardType = KeyboardType.Number, testTag = "socks_port_input"); Spacer(modifier = Modifier.width(8.dp)); IosInputField(label = "پورت HTTP", value = config.httpPort, onValueChange = { onUpdateConfig(config.copy(httpPort = it)) }, modifier = Modifier.width(75.dp), placeholder = "1820", keyboardType = KeyboardType.Number, testTag = "http_port_input") }
        AppDivider(); IosSwitchRow(icon = Icons.Default.Http, iconBg = IosActiveBlue, title = "پروکسی HTTP داخلی", subtitle = "نمایش یک پروکسی HTTP CONNECT در کنار SOCKS5", checked = config.httpProxyEnabled, onCheckedChange = { onUpdateConfig(config.copy(httpProxyEnabled = it)) }, testTag = "switch_http_proxy_enabled"); AppDivider()
        IosInputFieldRow(icon = Icons.Default.Code, iconBg = IosSecondaryLabel, label = "گروه‌های کلید TLS", value = config.tlsGroups, onValueChange = { onUpdateConfig(config.copy(tlsGroups = it)) }, placeholder = "P-256:X25519:P-384", testTag = "tls_groups_input"); AppDivider()
        if (isAndroid) { IosInputFieldRow(icon = Icons.Default.Dns, iconBg = IosActiveBlue, label = "سرورهای DNS تونل", value = config.dnsList, onValueChange = { onUpdateConfig(config.copy(dnsList = it.replace(Regex("\\s*,\\s*"), ","))) }, placeholder = "1.1.1.1,1.0.0.1", testTag = "dns_list_input"); AppDivider() }
        IosInputFieldRow(icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = AppPalette.accentVariant, label = "آی‌پی همتا اجباری", value = config.peer, onValueChange = { onUpdateConfig(config.copy(peer = it)) }, placeholder = "مثلاً 1.2.3.4:443", testTag = "peer_input"); AppDivider()
        IosSwitchRow(icon = Icons.Default.Bolt, iconBg = AppPalette.statusScanning, title = "بسته‌های نگهداری اتصال", subtitle = if (config.keepaliveEnabled) "حفظ اتصال NAT با پینگ‌های دوره‌ای" else "غیرفعال - بدون نگهداری اتصال (صرفه‌جویی در باتری)", checked = config.keepaliveEnabled, onCheckedChange = { onUpdateConfig(config.copy(keepaliveEnabled = it)) }, testTag = "switch_keepalive_enabled"); AppDivider()
        AnimatedVisibility(visible = config.keepaliveEnabled, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) { Column { IosInputFieldRow(icon = Icons.Default.Bolt, iconBg = AppPalette.statusScanning, label = "فاصله نگهداری اتصال (ثانیه)", value = config.keepalive.toString(), onValueChange = { onUpdateConfig(config.copy(keepalive = it.toIntOrNull() ?: 5)) }, placeholder = "5", keyboardType = KeyboardType.Number, testTag = "keepalive_input"); AppDivider() } }
        IosInputFieldRow(icon = Icons.Default.Timer, iconBg = IosSecondaryLabel, label = "فاصله اعتبارسنجی (ثانیه)", value = config.validateSecs.toString(), onValueChange = { onUpdateConfig(config.copy(validateSecs = it.toIntOrNull() ?: 10)) }, placeholder = "10", keyboardType = KeyboardType.Number, testTag = "validate_secs_input")
    } }
}

@Composable private fun SecurityPage(config: AetherConfig, isAndroid: Boolean, isBatteryOptimized: Boolean, onUpdateConfig: (AetherConfig) -> Unit, onRequestBatteryOptimization: () -> Unit) {
    IosGroupCard { Column {
        IosSwitchRow(icon = Icons.Default.VpnLock, iconBg = AppPalette.accentVariant, title = "قطع کننده اتصال سخت‌گیرانه", subtitle = "جلوگیری از هرگونه نشت حتی در هنگام توقف دستی", checked = config.strictKillSwitch, onCheckedChange = { onUpdateConfig(config.copy(strictKillSwitch = it)) }, testTag = "switch_strict_kill_switch"); AppDivider()
        IosSwitchRow(icon = Icons.Default.Lock, iconBg = AppPalette.statusError, title = "قطع کننده اتصال", subtitle = "مسدود کردن ترافیک وقتی VPN قطع است", checked = config.killSwitch, onCheckedChange = { onUpdateConfig(config.copy(killSwitch = it)) }, testTag = "switch_kill_switch"); AppDivider()
        IosSwitchRow(icon = Icons.Default.Security, iconBg = AppPalette.accentVariant, title = "محافظت در برابر نشت IPv6", subtitle = "اجبار همه ترافیک IPv6 از طریق تونل", checked = config.ipv6Leak, onCheckedChange = { onUpdateConfig(config.copy(ipv6Leak = it)) }, testTag = "switch_ipv6_leak"); AppDivider()
        IosSwitchRow(icon = Icons.Default.Restore, iconBg = IosActiveGreen, title = "اتصال مجدد هوشمند", subtitle = "تلاش برای بازیابی خودکار در صورت خرابی شبکه", checked = config.smartReconnect, onCheckedChange = { onUpdateConfig(config.copy(smartReconnect = it)) }, testTag = "switch_smart_reconnect")
        if (config.smartReconnect) { AppDivider(); Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Repeat, backgroundColor = IosSecondaryLabel); Spacer(modifier = Modifier.width(12.dp)); IosInputField(label = "حداکثر تلاش مجدد", value = config.reconnectRetryLimit.toString(), onValueChange = { onUpdateConfig(config.copy(reconnectRetryLimit = it.toIntOrNull() ?: 10)) }, placeholder = "10", keyboardType = KeyboardType.Number, testTag = "reconnect_limit_input") }; Spacer(modifier = Modifier.width(12.dp)); Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosInputField(label = "تاخیر (ثانیه)", value = config.reconnectSecs.toString(), onValueChange = { onUpdateConfig(config.copy(reconnectSecs = it.toIntOrNull() ?: 2)) }, placeholder = "2", keyboardType = KeyboardType.Number, testTag = "reconnect_secs_input") } } }
        AppDivider(); IosSwitchRow(icon = Icons.Default.Sync, iconBg = IosActiveGreen, title = "بازیابی Cloudflare", subtitle = "ثبت‌نام خودکار دستگاه جدید در صورت از دست رفتن هویت", checked = config.reprovision, onCheckedChange = { onUpdateConfig(config.copy(reprovision = it)) }, testTag = "switch_reprovision")
        if (isAndroid) { AppDivider(); IosSwitchRow(icon = Icons.Default.BatteryAlert, iconBg = AppPalette.statusError, title = "بهینه‌سازی باتری", subtitle = "اجازه اجرای AetherST بدون محدودیت", checked = isBatteryOptimized, enabled = !isBatteryOptimized, onCheckedChange = { if (it) onRequestBatteryOptimization() }, testTag = "switch_battery_opt") }
    } }
}

private data class ParsedUpstreamProxy(val scheme: String, val host: String, val port: String, val user: String, val pass: String)

private fun parseUpstreamProxy(raw: String): ParsedUpstreamProxy {
    if (raw.isBlank()) return ParsedUpstreamProxy("socks5", "127.0.0.1", "", "", "")
    val scheme = if (raw.startsWith("http://", ignoreCase = true)) "http" else "socks5"
    val body = raw.removePrefix("socks5://").removePrefix("http://")
    val authHost = if (body.contains("@")) body.substringBefore("@") to body.substringAfter("@") else "" to body
    val (userRaw, passRaw) = if (authHost.first.contains(":")) authHost.first.substringBefore(":") to authHost.first.substringAfter(":") else authHost.first to ""
    val hostPort = authHost.second
    val (host, port) = if (hostPort.startsWith("[")) {
        val end = hostPort.indexOf("]")
        hostPort.substring(0, end + 1) to hostPort.substringAfter("]:").substringBefore("/")
    } else {
        val i = hostPort.lastIndexOf(":")
        if (i < 0) hostPort to "" else hostPort.substring(0, i) to hostPort.substring(i + 1)
    }
    return ParsedUpstreamProxy(scheme, host, port, decodeUpstreamCredential(userRaw), decodeUpstreamCredential(passRaw))
}

private fun buildUpstreamProxy(p: ParsedUpstreamProxy): String {
    if (p.host.isBlank() || p.port.isBlank()) return ""
    val auth = if (p.user.isBlank()) "" else "${encodeUpstreamCredential(p.user)}:${encodeUpstreamCredential(p.pass)}@"
    return "${p.scheme}://$auth${p.host}:${p.port}"
}

private fun decodeUpstreamCredential(s: String): String = s.replace("%40", "@").replace("%3A", ":")
private fun encodeUpstreamCredential(s: String): String = s.replace("@", "%40").replace(":", "%3A")

@Composable private fun DiagnosticsPage(config: AetherConfig, onUpdateConfig: (AetherConfig) -> Unit) {
    IosGroupCard { Column {
        IosPickerRow(icon = Icons.Default.BugReport, iconBg = AppPalette.debugCyan, title = "ثبت وقایع برنامه", value = config.appLogLevel.displayName.substringBefore(" ("), options = AetherLogLevel.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(appLogLevel = AetherLogLevel.entries[it])) }); AppDivider()
        IosPickerRow(icon = Icons.Default.VpnLock, iconBg = IosSecondaryLabel, title = "ثبت وقایع هسته Aether", value = config.coreLogLevel.displayName.substringBefore(" ("), options = AetherLogLevel.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(coreLogLevel = AetherLogLevel.entries[it])) }); AppDivider()
        IosPickerRow(icon = Icons.Default.Speed, iconBg = IosActiveGreen, title = "پروفایل کارایی هسته", value = config.perfProfile.displayName, options = AetherPerfProfile.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(perfProfile = AetherPerfProfile.entries[it])) }); AppDivider()
        Column {
            IosSwitchRow(icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = AppPalette.accentVariantAlt, title = "زنجیره پروکسی خارجی", subtitle = "مسیریابی از طریق یک پروکسی SOCKS/HTTP محلی (برنامه دیگر)", checked = config.upstreamProxyEnabled, onCheckedChange = { onUpdateConfig(config.copy(upstreamProxyEnabled = it, upstreamProxy = if (it) config.upstreamProxy.ifBlank { "socks5://127.0.0.1:1080" } else "")) }, testTag = "switch_upstream_proxy")
            if (config.upstreamProxyEnabled) {
                AppDivider()
                val up = remember(config.upstreamProxy) { parseUpstreamProxy(config.upstreamProxy) }
                val updateUpstream: (ParsedUpstreamProxy) -> Unit = { onUpdateConfig(config.copy(upstreamProxy = buildUpstreamProxy(it))) }
                IosPickerRow(icon = Icons.Default.Shuffle, iconBg = AppPalette.accentVariantAlt, title = "نوع پروکسی", value = up.scheme.uppercase(), options = listOf("SOCKS5", "HTTP"), onOptionSelected = { idx -> updateUpstream(up.copy(scheme = if (idx == 0) "socks5" else "http")) })
                AppDivider()
                IosInputFieldRow(icon = Icons.Default.Dns, iconBg = AppPalette.textSecondary, label = "میزبان", value = up.host, onValueChange = { updateUpstream(up.copy(host = it)) }, placeholder = "127.0.0.1", testTag = "upstream_proxy_host")
                AppDivider()
                IosInputFieldRow(icon = Icons.Default.Numbers, iconBg = AppPalette.textSecondary, label = "پورت", value = up.port, onValueChange = { updateUpstream(up.copy(port = it.filter { c -> c.isDigit() }.take(5))) }, placeholder = "1080", keyboardType = KeyboardType.Number, testTag = "upstream_proxy_port")
                AppDivider()
                IosInputFieldRow(icon = Icons.Default.Person, iconBg = AppPalette.textSecondary, label = "نام کاربری (اختیاری)", value = up.user, onValueChange = { updateUpstream(up.copy(user = it)) }, placeholder = "user", testTag = "upstream_proxy_user")
                AppDivider()
                IosInputFieldRow(icon = Icons.Default.Lock, iconBg = AppPalette.textSecondary, label = "رمز عبور (اختیاری)", value = up.pass, onValueChange = { updateUpstream(up.copy(pass = it)) }, placeholder = "password", testTag = "upstream_proxy_pass")
            }
        }; AppDivider()
        IosSwitchRow(icon = Icons.AutoMirrored.Filled.Rule, iconBg = IosActiveBlue, title = "تشخیص دامنه", subtitle = "تشخیص SNI/Host برای قوانین مسیریابی دامنه", checked = config.routeSniffing, onCheckedChange = { onUpdateConfig(config.copy(routeSniffing = it)) }, testTag = "switch_route_sniffing")
        if (config.routeSniffing) { AppDivider(); IosInputFieldRow(icon = Icons.Default.Timer, iconBg = IosSecondaryLabel, label = "زمان انتظار تشخیص (ms)", value = config.sniffingTimeoutMs.toString(), onValueChange = { onUpdateConfig(config.copy(sniffingTimeoutMs = it.toIntOrNull() ?: 100)) }, placeholder = "100", keyboardType = KeyboardType.Number, testTag = "sniffing_timeout_input") }
        AppDivider(); IosSwitchRow(icon = Icons.Default.Restore, iconBg = IosActiveGreen, title = "استراتژی اتصال مجدد سریع", subtitle = "بهینه‌سازی زمان بازیابی جلسه", checked = config.quickReconnect, onCheckedChange = { onUpdateConfig(config.copy(quickReconnect = it)) }, testTag = "switch_quick_reconnect"); AppDivider()
        IosSwitchRow(icon = Icons.Default.Block, iconBg = AppPalette.statusError, title = "قفل سخت‌گیرانه پروفایل", subtitle = "غیرفعال‌سازی بازگشت به پروفایل‌های دیگر", checked = config.noProfileRetry, onCheckedChange = { onUpdateConfig(config.copy(noProfileRetry = it)) }, testTag = "switch_no_profile_retry")
    } }
}

@Composable private fun PsiphonPage(config: AetherConfig, onUpdateConfig: (AetherConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IosGroupCard { Column {
            IosSwitchRow(icon = Icons.Default.Shield, iconBg = AppPalette.accentVariant, title = "زنجیره سایفون", subtitle = if (config.psiphonEnabled) "فعال - مسیریابی از طریق سایفون برای آی‌پی غیرایرانی" else "غیرفعال", checked = config.psiphonEnabled, onCheckedChange = { onUpdateConfig(config.copy(psiphonEnabled = it, psiphonChainOuter = "masque")) }, testTag = "switch_psiphon_enabled")
            if (config.psiphonEnabled) {
                AppDivider()
                IosPickerRow(icon = Icons.Default.VpnLock, iconBg = AppPalette.statusConnected, title = "پروتکل بیرونی زنجیره", value = "masque", options = listOf("masque"), onOptionSelected = { onUpdateConfig(config.copy(psiphonChainOuter = "masque")) })
                AppDivider()
                val availableRegions by PsiphonEgressRegistry.availableRegions.collectAsStateWithLifecycle()
                val selectedRegion = config.psiphonEgressRegion.trim().uppercase()
                val regionCodes = buildList {
                    add("")
                    addAll(availableRegions)
                    if (selectedRegion.isNotEmpty() && selectedRegion !in availableRegions) add(selectedRegion)
                }
                val regionOptions = regionCodes.map { CountryNames.label(it) }
                IosPickerRow(icon = Icons.Default.Public, iconBg = Color(0xFF30B0C7), title = "موقعیت خروجی", value = CountryNames.label(selectedRegion), options = regionOptions, onOptionSelected = { idx -> onUpdateConfig(config.copy(psiphonEgressRegion = regionCodes[idx])) })
            }
        } }
        IosGroupCard {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("درباره زنجیره سایفون", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("سایفون به عنوان تونل داخلی بر روی پروتکل بیرونی انتخاب شده (مثلاً masque) زنجیره می‌شود. بیرونی عبور از سانسور را انجام می‌دهد، داخلی خروجی غیرایرانی را فراهم می‌کند. کلید را در داشبورد یا اینجا فعال کنید، پروتکل بیرونی را انتخاب کنید، سپس متصل شوید. فقط در اندروید کار می‌کند. وقتی غیرفعال است، اتصال از یک هسته Aether استفاده می‌کند.", color = IosSecondaryLabel, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable private fun HevEnginePage(config: AetherConfig, onUpdateConfig: (AetherConfig) -> Unit) {
    val hevLevels = listOf("error", "warn", "info", "debug")
    val levelLabels = mapOf("error" to "خطا", "warn" to "اخطار (پیش‌فرض)", "info" to "اطلاعات", "debug" to "اشکال‌زدایی (مفصل)")
    val currentLevel = if (config.hevLogLevel in hevLevels) config.hevLogLevel else "warn"
    val hevUdpOptions = listOf("udp", "icmp", "off")
    val hevUdpLabels = mapOf(
        "udp" to "UDP (ASSOCIATE)",
        "icmp" to "UDP (ICMP-in-TCP)",
        "off" to "غیرفعال"
    )
    val hevUdpMode = if (config.hevUdpMode in hevUdpOptions) config.hevUdpMode else "udp"

    IosGroupCard { Column {
        IosPickerRow(
            icon = Icons.Default.BugReport,
            iconBg = AppPalette.accentVariantAlt,
            title = "سطح گزارش HEV",
            value = levelLabels[currentLevel] ?: "اخطار (پیش‌فرض)",
            options = hevLevels.map { levelLabels[it]!! },
            onOptionSelected = { index -> onUpdateConfig(config.copy(hevLogLevel = hevLevels[index])) }
        ); AppDivider()
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                IosIconBadge(icon = Icons.Default.Timer, backgroundColor = IosActiveBlue); Spacer(modifier = Modifier.width(12.dp))
                IosInputField(label = "زمان انتظار اتصال (ms)", value = config.hevConnectTimeoutMs.toString(), onValueChange = { onUpdateConfig(config.copy(hevConnectTimeoutMs = it.toIntOrNull()?.coerceIn(500, 120000) ?: 5000)) }, modifier = Modifier.weight(1f), placeholder = "5000", keyboardType = KeyboardType.Number, testTag = "hev_connect_timeout_input")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                IosIconBadge(icon = Icons.Default.SwapHoriz, backgroundColor = IosActiveGreen); Spacer(modifier = Modifier.width(12.dp))
                IosInputField(label = "زمان انتظار خواندن/نوشتن (ms)", value = config.hevReadWriteTimeoutMs.toString(), onValueChange = { onUpdateConfig(config.copy(hevReadWriteTimeoutMs = it.toIntOrNull()?.coerceIn(1000, 600000) ?: 60000)) }, modifier = Modifier.weight(1f), placeholder = "60000", keyboardType = KeyboardType.Number, testTag = "hev_rw_timeout_input")
            }
        }
        AppDivider()
        IosInputFieldRow(icon = Icons.Default.Layers, iconBg = AppPalette.accentVariant, label = "حداکثر جلسات (0 = نامحدود)", value = config.hevMaxSessionCount.toString(), onValueChange = { onUpdateConfig(config.copy(hevMaxSessionCount = it.toIntOrNull()?.coerceIn(0, 200000) ?: 0)) }, placeholder = "0", keyboardType = KeyboardType.Number, testTag = "hev_max_sessions_input"); AppDivider()
        IosInputFieldRow(icon = Icons.Default.Storage, iconBg = AppPalette.statusScanning, label = "اندازه حافظه نهان MapDNS", value = config.hevMapdnsCacheSize.toString(), onValueChange = { onUpdateConfig(config.copy(hevMapdnsCacheSize = it.toIntOrNull()?.coerceIn(100, 1000000) ?: 10000)) }, placeholder = "10000", keyboardType = KeyboardType.Number, testTag = "hev_mapdns_cache_input")
        AppDivider()
        IosPickerRow(
            icon = Icons.Default.SwapVert,
            iconBg = Color(0xFF30B0C7),
            title = "حالت ارسال UDP",
            value = hevUdpLabels[hevUdpMode] ?: "UDP (ASSOCIATE)",
            options = hevUdpOptions.map { hevUdpLabels[it]!! },
            onOptionSelected = { index -> onUpdateConfig(config.copy(hevUdpMode = hevUdpOptions[index])) }
        )
    } }
    Spacer(modifier = Modifier.height(8.dp))
    IosGroupCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text("درباره موتور HEV", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("این مقادیر موتور بومی HEV tun2socks را که در حالت تونل در اندروید استفاده می‌شود، پیکربندی می‌کنند. تغییرات پس از اتصال مجدد VPN اعمال می‌شوند. زمان‌های انتظار در محدوده‌های ایمن محدود شده‌اند؛ جلسات حداکثر 0 به معنای نامحدود است.", color = IosSecondaryLabel, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable private fun SystemPage(isAndroid: Boolean, onExportBackup: () -> Unit, onImportBackup: () -> Unit, onOpenVpnSettings: () -> Unit, onResetClick: () -> Unit) {
    IosGroupCard { Column {
        if (isAndroid) {
            IosActionRow(icon = Icons.Default.Lock, iconBg = Color(0xFF0A84FF), title = "VPN همیشه روشن", subtitle = "باز کردن تنظیمات سیستم VPN برای始终保持 AetherST متصل", onClick = onOpenVpnSettings); AppDivider()
        }
        IosActionRow(icon = Icons.Default.CloudUpload, iconBg = AppPalette.accentVariant, title = "پشتیبان‌گیری کامل تنظیمات", subtitle = "خروجی همه تنظیمات به فایل .astf", onClick = onExportBackup); AppDivider()
        IosActionRow(icon = Icons.Default.CloudDownload, iconBg = IosActiveGreen, title = "بازیابی از پشتیبان", subtitle = "وارد کردن تنظیمات از فایل .astf", onClick = onImportBackup); AppDivider()
        IosActionRow(icon = Icons.Default.DeleteForever, iconBg = AppPalette.statusError, title = "بازنشانی به حالت پیش‌فرض کارخانه", subtitle = "پاک کردن همه تنظیمات سفارشی و راه‌اندازی مجدد", onClick = onResetClick, titleColor = AppPalette.statusError)
    } }
}
