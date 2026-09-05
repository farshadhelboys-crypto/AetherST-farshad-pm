package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.immaghzbad.aetherst.platform.*
import io.github.immaghzbad.aetherst.shared.data.*
import io.github.immaghzbad.aetherst.shared.model.*
import io.github.immaghzbad.aetherst.shared.ui.components.CountryFlag
import io.github.immaghzbad.aetherst.shared.ui.theme.AppPalette
import io.github.immaghzbad.aetherst.shared.util.CountryNames
import io.github.immaghzbad.aetherst.subscription.PlatformSubscriptionCard
import io.github.immaghzbad.aetherst.subscription.SubscriptionViewModel
import kotlinx.coroutines.launch

// ==================== CONSTANTS ====================
private const val TELEGRAM_CHANNEL_URL = "https://t.me/farshad_pm_org"

// ==================== COLORS ====================
private val IosCardBg = AppPalette.surfaceRaised
private val IosGroupBg = AppPalette.divider
private val IosSecondaryLabel = AppPalette.textSecondary
private val IosActiveGreen = AppPalette.statusConnected
private val IosActiveBlue = AppPalette.accent
private val IosScanningAmber = AppPalette.statusScanning
private val IosErrorRed = AppPalette.statusError

// ==================== MAIN SCREEN ====================
@Composable
fun DashboardScreen(
    config: AetherConfig,
    connectionStatus: ConnectionStatus,
    elapsedSeconds: Long,
    sessionTraffic: SessionTraffic,
    ipInfo: IpInfo = IpInfo(),
    pingState: PingState = PingState(),
    appVersion: String = "1.0.0",
    onToggleVpn: () -> Unit,
    onForceStop: () -> Unit = {},
    onUpdateConfig: (AetherConfig) -> Unit = {},
    onUpdateProtocol: (AetherProtocol) -> Unit,
    onTogglePsiphon: (Boolean) -> Unit = {},
    onRefreshIpInfo: () -> Unit = {},
    onRefreshPing: () -> Unit = {},
    onCopy: (String) -> Unit = {},
    onOpenSettingsToZeroTrust: () -> Unit = {},
    onShowToast: (String, Boolean) -> Unit = { _, _ -> },
    bottomContentPadding: Dp = 0.dp,
    platformContext: PlatformContext? = null,
    subscriptionInfo: SubscriptionInfo? = null
) {
    // ========== STATE ==========
    var showProxyOverlay by remember { mutableStateOf(true) }
    var showAdminRequiredDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var supportDialogAuto by remember { mutableStateOf(true) }
    
    val uriHandler = LocalUriHandler.current
    val settings = platformContext?.let { getSettings(it) }
    val systemUtils = platformContext?.let { getSystemUtils(it) }
    val scope = rememberCoroutineScope()
    
    // ========== SIDE EFFECTS ==========
    LaunchedEffect(Unit) {
        if (settings != null && !settings.getBoolean("support_dialog_dismissed", false)) {
            supportDialogAuto = true
            showSupportDialog = true
        }
    }

    LaunchedEffect(connectionStatus) {
        if (connectionStatus != ConnectionStatus.RUNNING) {
            showProxyOverlay = true
        }
    }

    // ========== COMPUTED ==========
    val isWindows = remember { 
        try { 
            System.getProperty("os.name")?.lowercase()?.contains("win") == true 
        } catch (_: Throwable) { 
            false 
        } 
    }
    val isDesktop = isDesktop()

    // ========== HANDLERS ==========
    val handleToggle: () -> Boolean = {
        val isSubActive = subscriptionInfo?.type == "paid" && 
                          (subscriptionInfo?.expiresAtMillis ?: 0L) > System.currentTimeMillis()
        
        when {
            connectionStatus == ConnectionStatus.STOPPED && !isSubActive -> {
                onShowToast("اشتراک شما فعال نیست، لطفاً کد فعال‌سازی را وارد کنید.", true)
                false
            }
            connectionStatus == ConnectionStatus.STOPPING -> {
                onForceStop()
                true
            }
            config.protocol == AetherProtocol.ZERO_TRUST && connectionStatus == ConnectionStatus.STOPPED -> {
                if (config.zeroTrustError() != null) {
                    onOpenSettingsToZeroTrust()
                    false
                } else {
                    onToggleVpn()
                    true
                }
            }
            isWindows && config.connectionMode == ConnectionMode.TUNNEL && systemUtils?.isAdministrator() == false -> {
                showAdminRequiredDialog = true
                false
            }
            else -> {
                onToggleVpn()
                true
            }
        }
    }

    // ========== LAYOUT ==========
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight
        val scaleFactor = (screenWidth.value / 411f).coerceIn(0.7f, 1.1f)
        val isCompactHeight = screenHeight < 640.dp
        val isVeryCompactHeight = screenHeight < 580.dp
        val horizontalPadding = if (screenWidth < 360.dp) 12.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = bottomContentPadding + 12.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ========== TOP SECTION ==========
            TopSection(
                config = config,
                connectionStatus = connectionStatus,
                appVersion = appVersion,
                isDesktop = isDesktop,
                scaleFactor = scaleFactor,
                onShowSupportDialog = { 
                    supportDialogAuto = false
                    showSupportDialog = true 
                }
            )

            // ========== MIDDLE SECTION ==========
            Column(
                verticalArrangement = Arrangement.spacedBy((14 * scaleFactor).dp)
            ) {
                // Status Card
                IosStatusHeroCard(
                    connectionStatus = connectionStatus,
                    elapsedSeconds = elapsedSeconds,
                    sessionTraffic = sessionTraffic,
                    config = config,
                    ipInfo = ipInfo,
                    pingState = pingState,
                    onRefreshIpInfo = onRefreshIpInfo,
                    onRefreshPing = onRefreshPing,
                    onCopy = onCopy,
                    hideConfigChips = isCompactHeight,
                    scaleFactor = scaleFactor
                )
                
                // Subscription Card (Desktop only)
                if (!isDesktop) {
                    PlatformSubscriptionCard()
                }

                // Error Message
                if (!isVeryCompactHeight && connectionStatus == ConnectionStatus.ERROR) {
                    ErrorCard(scaleFactor = scaleFactor)
                }
            }

            // ========== BOTTOM SECTION ==========
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
            ) {
                // Power Button
                PowerButtonSection(
                    connectionStatus = connectionStatus,
                    config = config,
                    isWindows = isWindows,
                    isDesktop = isDesktop,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    scaleFactor = scaleFactor,
                    handleToggle = handleToggle,
                    onForceStop = onForceStop,
                    onShowAdminDialog = { showAdminRequiredDialog = true },
                    systemUtils = systemUtils
                )

                // Settings Cards
                if (!isVeryCompactHeight) {
                    SettingsSection(
                        config = config,
                        connectionStatus = connectionStatus,
                        isDesktop = isDesktop,
                        scaleFactor = scaleFactor,
                        onTogglePsiphon = onTogglePsiphon,
                        onUpdateConfig = onUpdateConfig,
                        onUpdateProtocol = onUpdateProtocol
                    )
                }
            }
        }

        // ========== OVERLAYS ==========
        ProxyOverlay(
            config = config,
            connectionStatus = connectionStatus,
            showProxyOverlay = showProxyOverlay,
            onHide = { showProxyOverlay = false },
            onCopy = onCopy,
            scaleFactor = scaleFactor
        )

        AdminRequiredDialog(
            showDialog = showAdminRequiredDialog,
            onRelaunch = {
                showAdminRequiredDialog = false
                systemUtils?.relaunchAsAdmin()
            },
            onDismiss = { showAdminRequiredDialog = false },
            scaleFactor = scaleFactor
        )

        SupportDialog(
            showDialog = showSupportDialog,
            autoShow = supportDialogAuto,
            onJoin = {
                settings?.putBoolean("support_dialog_dismissed", true)
                showSupportDialog = false
                uriHandler.openUri(TELEGRAM_CHANNEL_URL)
            },
            onSkip = {
                settings?.putBoolean("support_dialog_dismissed", true)
                showSupportDialog = false
            },
            onCancel = { showSupportDialog = false },
            scaleFactor = scaleFactor
        )
    }
}

// ==================== TOP SECTION ====================
@Composable
private fun TopSection(
    config: AetherConfig,
    connectionStatus: ConnectionStatus,
    appVersion: String,
    isDesktop: Boolean,
    scaleFactor: Float,
    onShowSupportDialog: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isDesktop) 12.dp else 36.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Feri Pm Tunnel",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = (26 * scaleFactor).sp,
                lineHeight = (30 * scaleFactor).sp
            )
            Text(
                text = if (config.connectionMode == ConnectionMode.TUNNEL) "تونل امن و خصوصی" else "پروکسی محلی با کارایی بالا",
                style = MaterialTheme.typography.bodySmall,
                color = IosSecondaryLabel,
                fontSize = (12 * scaleFactor).sp,
                lineHeight = (16 * scaleFactor).sp
            )
            if (config.protocol == AetherProtocol.ZERO_TRUST && 
                connectionStatus == ConnectionStatus.RUNNING && 
                config.teamName.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                ZeroTrustBadge(config = config, scaleFactor = scaleFactor)
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (config.connectionMode == ConnectionMode.PROXY_ONLY && 
                connectionStatus == ConnectionStatus.RUNNING) {
                IconButton(
                    onClick = { /* handled in proxy overlay */ },
                    modifier = Modifier.size((32 * scaleFactor).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "اطلاعات پروکسی",
                        tint = IosActiveBlue,
                        modifier = Modifier.size((22 * scaleFactor).dp)
                    )
                }
                Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
            }
            
            Surface(
                shape = RoundedCornerShape(50),
                color = IosGroupBg,
                modifier = Modifier.clickable { onShowSupportDialog() }
            ) {
                Text(
                    text = "نسخه $appVersion",
                    modifier = Modifier.padding(
                        horizontal = (12 * scaleFactor).dp, 
                        vertical = (6 * scaleFactor).dp
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = IosActiveBlue,
                    fontSize = (10 * scaleFactor).sp
                )
            }
        }
    }
}

// ==================== ZERO TRUST BADGE ====================
@Composable
private fun ZeroTrustBadge(config: AetherConfig, scaleFactor: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.VerifiedUser, 
            null, 
            tint = IosActiveGreen, 
            modifier = Modifier.size((14 * scaleFactor).dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = buildString {
                append(config.teamName)
                val who = config.accessEmail.ifBlank { 
                    config.accessId.ifBlank { 
                        config.accessToken.takeIf { it.isNotBlank() }?.let { "توکن" } 
                    } 
                }
                if (!who.isNullOrBlank()) append(" • $who")
            },
            style = MaterialTheme.typography.bodySmall,
            color = IosActiveGreen,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * scaleFactor).sp
        )
    }
}

// ==================== ERROR CARD ====================
@Composable
private fun ErrorCard(scaleFactor: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = IosErrorRed.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Refresh, 
                null, 
                tint = IosErrorRed, 
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "اتصال برقرار نشد، لطفاً دوباره تلاش کنید.",
                color = IosErrorRed,
                fontSize = (11 * scaleFactor).sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==================== POWER BUTTON SECTION ====================
@Composable
private fun PowerButtonSection(
    connectionStatus: ConnectionStatus,
    config: AetherConfig,
    isWindows: Boolean,
    isDesktop: Boolean,
    screenWidth: Dp,
    screenHeight: Dp,
    scaleFactor: Float,
    handleToggle: () -> Boolean,
    onForceStop: () -> Unit,
    onShowAdminDialog: () -> Unit,
    systemUtils: SystemUtils?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            config.connectButtonStyle == "capsule" -> {
                CapsuleConnectButton(
                    connectionStatus = connectionStatus,
                    onToggle = handleToggle,
                    onRecover = onForceStop,
                    modifier = Modifier.fillMaxWidth(),
                    scaleFactor = scaleFactor
                )
            }
            (isDesktop && isWindows) || !isDesktop -> {
                WindowsSwipeSwitch(
                    connectionStatus = connectionStatus,
                    onToggle = handleToggle,
                    onRecover = onForceStop,
                    onAdminCancelResetKey = 0,
                    modifier = Modifier.fillMaxWidth(),
                    scaleFactor = scaleFactor
                )
            }
            else -> {
                val minDim = if (screenWidth < screenHeight) screenWidth else screenHeight
                val buttonSize = (minDim * 0.28f).coerceIn(90.dp, 140.dp)
                IosPowerButton(
                    connectionStatus = connectionStatus,
                    onToggle = { handleToggle().let {} },
                    onRecover = onForceStop,
                    size = buttonSize
                )
            }
        }
    }
}

// ==================== SETTINGS SECTION ====================
@Composable
private fun SettingsSection(
    config: AetherConfig,
    connectionStatus: ConnectionStatus,
    isDesktop: Boolean,
    scaleFactor: Float,
    onTogglePsiphon: (Boolean) -> Unit,
    onUpdateConfig: (AetherConfig) -> Unit,
    onUpdateProtocol: (AetherProtocol) -> Unit
) {
    // Psiphon Card (Mobile only)
    if (!isDesktop) {
        PsiphonCard(
            config = config,
            connectionStatus = connectionStatus,
            scaleFactor = scaleFactor,
            onTogglePsiphon = onTogglePsiphon,
            onUpdateConfig = onUpdateConfig
        )
    }
    
    // Connection Mode (Windows Desktop only)
    if (isDesktop && isWindows()) {
        IosConnectionModeSegmentedControl(
            selectedMode = config.connectionMode,
            onModeSelected = { onUpdateConfig(config.copy(connectionMode = it)) },
            enabled = connectionStatus == ConnectionStatus.STOPPED || connectionStatus == ConnectionStatus.ERROR,
            scaleFactor = scaleFactor
        )
    }
    
    // Protocol Selector
    IosProtocolSegmentedControl(
        selectedProtocol = config.protocol,
        onProtocolSelected = onUpdateProtocol,
        enabled = connectionStatus == ConnectionStatus.STOPPED || connectionStatus == ConnectionStatus.ERROR,
        allowedProtocols = if (config.psiphonEnabled) setOf(AetherProtocol.MASQUE) else null,
        scaleFactor = scaleFactor
    )
}

// ==================== PSIPHON CARD ====================
@Composable
private fun PsiphonCard(
    config: AetherConfig,
    connectionStatus: ConnectionStatus,
    scaleFactor: Float,
    onTogglePsiphon: (Boolean) -> Unit,
    onUpdateConfig: (AetherConfig) -> Unit
) {
    val psiphonAllowed = config.protocol == AetherProtocol.MASQUE
    val psiphonOn = config.psiphonEnabled && psiphonAllowed
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppPalette.accentVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield, 
                            null, 
                            tint = Color.White, 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "زنجیره سایفون",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = (13 * scaleFactor).sp
                        )
                        Text(
                            if (psiphonAllowed) "مسیریابی از طریق سایفون برای آی‌پی غیرایرانی" 
                            else "فقط با پروتکل MASQUE در دسترس است",
                            color = IosSecondaryLabel,
                            fontSize = (10 * scaleFactor).sp
                        )
                    }
                }
                Switch(
                    checked = psiphonOn,
                    onCheckedChange = { onTogglePsiphon(it) },
                    enabled = psiphonAllowed && 
                             (connectionStatus == ConnectionStatus.STOPPED || 
                              connectionStatus == ConnectionStatus.ERROR),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = IosActiveGreen,
                        checkedBorderColor = Color.Transparent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = AppPalette.inactiveTrack,
                        uncheckedBorderColor = Color.Transparent,
                        disabledCheckedTrackColor = IosActiveGreen.copy(alpha = 0.4f),
                        disabledCheckedThumbColor = Color.White.copy(alpha = 0.9f),
                        disabledCheckedBorderColor = Color.Transparent,
                        disabledUncheckedTrackColor = AppPalette.inactiveTrack.copy(alpha = 0.6f),
                        disabledUncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                        disabledUncheckedBorderColor = Color.Transparent
                    )
                )
            }
            
            if (psiphonOn) {
                PsiphonRegionSelector(
                    config = config,
                    scaleFactor = scaleFactor,
                    onUpdateConfig = onUpdateConfig
                )
            }
        }
    }
}

// ==================== PSIPHON REGION SELECTOR ====================
@Composable
private fun PsiphonRegionSelector(
    config: AetherConfig,
    scaleFactor: Float,
    onUpdateConfig: (AetherConfig) -> Unit
) {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.1f), 
        thickness = 0.5.dp, 
        modifier = Modifier.padding(start = 50.dp)
    )
    
    val availableRegions by PsiphonEgressRegistry.availableRegions.collectAsStateWithLifecycle()
    val selectedRegion = config.psiphonEgressRegion.trim().uppercase()
    
    val regionCodes = buildList {
        add("")
        addAll(availableRegions)
        if (selectedRegion.isNotEmpty() && selectedRegion !in availableRegions) {
            add(selectedRegion)
        }
    }
    
    val regionOptions = regionCodes.map { CountryNames.label(it) }
    
    IosPickerRow(
        icon = Icons.Default.Public,
        iconBg = Color(0xFF30B0C7),
        title = "موقعیت خروجی",
        value = CountryNames.label(selectedRegion),
        options = regionOptions,
        onOptionSelected = { idx -> 
            onUpdateConfig(config.copy(psiphonEgressRegion = regionCodes[idx])) 
        }
    )
}

// ==================== PROXY OVERLAY ====================
@Composable
private fun ProxyOverlay(
    config: AetherConfig,
    connectionStatus: ConnectionStatus,
    showProxyOverlay: Boolean,
    onHide: () -> Unit,
    onCopy: (String) -> Unit,
    scaleFactor: Float
) {
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(showProxyOverlay) {
        if (showProxyOverlay) {
            offsetY.snapTo(0f)
        }
    }

    AnimatedVisibility(
        visible = config.connectionMode == ConnectionMode.PROXY_ONLY && 
                 connectionStatus == ConnectionStatus.RUNNING && 
                 showProxyOverlay,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 36.dp)
            .graphicsLayer { translationY = offsetY.value }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (offsetY.value < -100f) {
                                onHide()
                            } else {
                                offsetY.animateTo(
                                    0f, 
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                            }
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        scope.launch {
                            offsetY.snapTo((offsetY.value + dragAmount).coerceAtMost(20f))
                        }
                    }
                )
            }
    ) {
        ProxyOverlayPill(
            host = config.socksHost,
            socksPort = config.socksPort,
            httpPort = config.httpPort,
            onHide = onHide,
            onCopy = onCopy,
            scaleFactor = scaleFactor
        )
    }
}

// ==================== SUPPORT DIALOG ====================
@Composable
private fun SupportDialog(
    showDialog: Boolean,
    autoShow: Boolean,
    onJoin: () -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit,
    scaleFactor: Float
) {
    if (!showDialog) return
    
    Dialog(
        onDismissRequest = { if (autoShow) onSkip() else onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding((20 * scaleFactor).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "پشتیبانی Feri Pm Tunnel",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (18 * scaleFactor).sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height((10 * scaleFactor).dp))
                    Text(
                        "برای اطلاع از به‌روزرسانی‌ها می‌توانید مستقیماً به پیوی سازنده پیام دهید",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = (13 * scaleFactor).sp,
                        lineHeight = (18 * scaleFactor).sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height((20 * scaleFactor).dp))
                    Button(
                        onClick = onJoin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((48 * scaleFactor).dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IosActiveBlue, 
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send, 
                            null, 
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "عضویت در کانال تلگرام",
                            fontWeight = FontWeight.Bold,
                            fontSize = (14 * scaleFactor).sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
                    TextButton(
                        onClick = { if (autoShow) onSkip() else onCancel() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((42 * scaleFactor).dp)
                    ) {
                        Text(
                            if (autoShow) "رد کردن" else "انصراف",
                            color = IosSecondaryLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = (13 * scaleFactor).sp
                        )
                    }
                }
            }
        }
    }
}

// ==================== ADMIN REQUIRED DIALOG ====================
@Composable
fun AdminRequiredDialog(
    showDialog: Boolean,
    onRelaunch: () -> Unit,
    onDismiss: () -> Unit,
    scaleFactor: Float = 1f
) {
    if (!showDialog) return
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = (24 * scaleFactor).dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = (340 * scaleFactor).dp)
                    .fillMaxWidth()
                    .clickable(enabled = false) { },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.surfaceRaised),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding((24 * scaleFactor).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size((64 * scaleFactor).dp)
                            .clip(CircleShape)
                            .background(AppPalette.statusError.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AppPalette.statusError,
                            modifier = Modifier.size((32 * scaleFactor).dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height((20 * scaleFactor).dp))
                    
                    Text(
                        text = "نیاز به دسترسی مدیر",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = (20 * scaleFactor).sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height((12 * scaleFactor).dp))
                    
                    Text(
                        text = "حالت TUN برای ایجاد رابط شبکه مجازی به دسترسی مدیر نیاز دارد. لطفاً برنامه را به عنوان مدیر اجرا کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = (14 * scaleFactor).sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height((32 * scaleFactor).dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                    ) {
                        Button(
                            onClick = onRelaunch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((52 * scaleFactor).dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppPalette.accent,
                                contentColor = Color.White
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FlashOn, 
                                    null, 
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اجرا به عنوان مدیر",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (15 * scaleFactor).sp
                                )
                            }
                        }
                        
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((52 * scaleFactor).dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "انصراف",
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium,
                                fontSize = (15 * scaleFactor).sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== PROXY OVERLAY PILL ====================
@Composable
fun ProxyOverlayPill(
    host: String,
    socksPort: String,
    httpPort: String,
    onHide: () -> Unit,
    onCopy: (String) -> Unit,
    scaleFactor: Float
) {
    val socksAddress = "$host:$socksPort"
    val httpAddress = "$host:$httpPort"

    Surface(
        modifier = Modifier
            .widthIn(max = 400.dp)
            .padding(horizontal = 8.dp)
            .shadow(
                24.dp, 
                RoundedCornerShape(20.dp), 
                spotColor = IosActiveBlue.copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = AppPalette.surfaceRaised.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(IosActiveBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Dns, 
                    null, 
                    tint = IosActiveBlue, 
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f), 
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ProxyCopyRow(
                    label = "SOCKS5",
                    address = socksAddress,
                    onCopy = { onCopy(socksAddress) },
                    scaleFactor = scaleFactor
                )
                ProxyCopyRow(
                    label = "HTTP",
                    address = httpAddress,
                    onCopy = { onCopy(httpAddress) },
                    scaleFactor = scaleFactor
                )
            }

            VerticalDivider(
                modifier = Modifier.height(36.dp), 
                thickness = 1.dp, 
                color = Color.White.copy(alpha = 0.1f)
            )

            IconButton(
                onClick = onHide,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close, 
                    null, 
                    tint = IosSecondaryLabel, 
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==================== PROXY COPY ROW ====================
@Composable
private fun ProxyCopyRow(
    label: String,
    address: String,
    onCopy: () -> Unit,
    scaleFactor: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCopy() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall,
                color = IosActiveBlue,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (9 * scaleFactor).sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (12 * scaleFactor).sp,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "کپی",
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size((14 * scaleFactor).dp)
        )
    }
}

// ==================== STATUS HERO CARD ====================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosStatusHeroCard(
    connectionStatus: ConnectionStatus,
    elapsedSeconds: Long,
    sessionTraffic: SessionTraffic,
    config: AetherConfig,
    ipInfo: IpInfo = IpInfo(),
    pingState: PingState = PingState(),
    onRefreshIpInfo: () -> Unit = {},
    onRefreshPing: () -> Unit = {},
    onCopy: (String) -> Unit = {},
    hideConfigChips: Boolean = false,
    scaleFactor: Float = 1f
) {
    val statusColor by animateColorAsState(
        targetValue = when (connectionStatus) {
            ConnectionStatus.RUNNING, ConnectionStatus.TUN_ACTIVE -> IosActiveGreen
            ConnectionStatus.STARTING, ConnectionStatus.VALIDATING, 
            ConnectionStatus.DATAPLANE_VALIDATED, ConnectionStatus.SOCKS_READY, 
            ConnectionStatus.RECONNECTING, ConnectionStatus.STOPPING -> IosScanningAmber
            ConnectionStatus.ERROR, ConnectionStatus.FAILED -> IosErrorRed
            ConnectionStatus.STOPPED -> IosSecondaryLabel
        },
        label = "statusColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("status_hero_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding((14 * scaleFactor).dp)
        ) {
            Column {
                // Status Row
                StatusRow(
                    connectionStatus = connectionStatus,
                    config = config,
                    statusColor = statusColor,
                    scaleFactor = scaleFactor
                )

                Spacer(modifier = Modifier.height((10 * scaleFactor).dp))

                // Time & Ping Row
                TimePingRow(
                    connectionStatus = connectionStatus,
                    elapsedSeconds = elapsedSeconds,
                    pingState = pingState,
                    onRefreshPing = onRefreshPing,
                    scaleFactor = scaleFactor
                )

                Spacer(modifier = Modifier.height((8 * scaleFactor).dp))

                // Traffic Row
                if (connectionStatus == ConnectionStatus.RUNNING) {
                    TrafficRow(
                        sessionTraffic = sessionTraffic,
                        scaleFactor = scaleFactor
                    )
                }

                Spacer(modifier = Modifier.height((8 * scaleFactor).dp))

                // IP Info Row
                IpInfoRow(
                    ipInfo = ipInfo,
                    onRefreshIpInfo = onRefreshIpInfo,
                    onCopy = onCopy,
                    scaleFactor = scaleFactor
                )

                // Config Chips
                if (!hideConfigChips) {
                    Spacer(modifier = Modifier.height((10 * scaleFactor).dp))
                    ConfigChipsRow(
                        config = config,
                        scaleFactor = scaleFactor
                    )
                }
            }
        }
    }
}

// ==================== STATUS ROW ====================
@Composable
private fun StatusRow(
    connectionStatus: ConnectionStatus,
    config: AetherConfig,
    statusColor: Color,
    scaleFactor: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size((7 * scaleFactor).dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width((5 * scaleFactor).dp))
            Text(
                text = when (connectionStatus) {
                    ConnectionStatus.RUNNING, ConnectionStatus.TUN_ACTIVE -> 
                        if (config.connectionMode == ConnectionMode.TUNNEL) "محافظت شده و متصل" else "پروکسی فعال"
                    ConnectionStatus.STARTING -> "در حال اتصال به بهترین سرور..."
                    ConnectionStatus.VALIDATING, ConnectionStatus.DATAPLANE_VALIDATED -> "برقراری ارتباط..."
                    ConnectionStatus.SOCKS_READY -> "در حال اتصال..."
                    ConnectionStatus.RECONNECTING -> "تلاش مجدد..."
                    ConnectionStatus.STOPPING -> "برای توقف اجباری بکشید"
                    ConnectionStatus.ERROR, ConnectionStatus.FAILED -> "خطا در اتصال یا اینترنت"
                    ConnectionStatus.STOPPED -> "آماده اتصال"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = statusColor,
                fontSize = (8.5 * scaleFactor).sp
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = IosGroupBg
        ) {
            val protocolText = if (config.protocol == AetherProtocol.MASQUE) {
                if (config.h2Mode) "MASQUE (H2)" else "MASQUE (H3)"
            } else {
                config.protocol.displayName
            }
            Text(
                text = protocolText,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = IosActiveBlue,
                fontSize = (8.5 * scaleFactor).sp
            )
        }
    }
}

// ==================== TIME PING ROW ====================
@Composable
private fun TimePingRow(
    connectionStatus: ConnectionStatus,
    elapsedSeconds: Long,
    pingState: PingState,
    onRefreshPing: () -> Unit,
    scaleFactor: Float
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text = formatTime(elapsedSeconds),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = (28 * scaleFactor).sp
            )
        }

        if (connectionStatus == ConnectionStatus.RUNNING) {
            PingIndicator(
                pingState = pingState,
                onRefreshPing = onRefreshPing,
                scaleFactor = scaleFactor
            )
        } else {
            Text(
                text = if (connectionStatus == ConnectionStatus.RECONNECTING) "تلاش مجدد" else "بدون ارتباط",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (connectionStatus == ConnectionStatus.RECONNECTING) IosScanningAmber else IosSecondaryLabel,
                modifier = Modifier.clickable { onRefreshPing() },
                fontSize = (10 * scaleFactor).sp
            )
        }
    }
}

// ==================== PING INDICATOR ====================
@Composable
private fun PingIndicator(
    pingState: PingState,
    onRefreshPing: () -> Unit,
    scaleFactor: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onRefreshPing() }
            .padding(2.dp)
    ) {
        if (pingState.isPinging) {
            CircularProgressIndicator(
                modifier = Modifier.size((11 * scaleFactor).dp),
                color = IosActiveBlue,
                strokeWidth = 1.5.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "پینگ",
                tint = if (pingState.error != null) IosErrorRed else IosActiveBlue,
                modifier = Modifier.size((15 * scaleFactor).dp)
            )
        }
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = when {
                pingState.isPinging -> "..."
                pingState.error != null -> "عدم پاسخ"
                pingState.ms >= 0 -> "${pingState.ms}ms"
                else -> "پینگ"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (pingState.error != null) IosErrorRed else IosActiveBlue,
            fontSize = (12 * scaleFactor).sp
        )
    }
}

// ==================== TRAFFIC ROW ====================
@Composable
private fun TrafficRow(
    sessionTraffic: SessionTraffic,
    scaleFactor: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(IosGroupBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TrafficValue(
            label = "ارسال",
            value = formatTrafficBytes(sessionTraffic.uploadedBytes),
            speed = sessionTraffic.uploadSpeedBps,
            color = IosActiveBlue,
            alignment = Alignment.Start,
            modifier = Modifier.weight(1f),
            scaleFactor = scaleFactor
        )
        TrafficValue(
            label = "دریافت",
            value = formatTrafficBytes(sessionTraffic.downloadedBytes),
            speed = sessionTraffic.downloadSpeedBps,
            color = IosActiveGreen,
            alignment = Alignment.End,
            modifier = Modifier.weight(1f),
            scaleFactor = scaleFactor
        )
    }
}

// ==================== IP INFO ROW ====================
@Composable
private fun IpInfoRow(
    ipInfo: IpInfo,
    onRefreshIpInfo: () -> Unit,
    onCopy: (String) -> Unit,
    scaleFactor: Float
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { onRefreshIpInfo() },
                onLongClick = {
                    if (ipInfo.ip.isNotEmpty()) {
                        onCopy(ipInfo.ip)
                    }
                }
            ),
        color = IosGroupBg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (ipInfo.countryCode.isNotEmpty()) {
                    CountryFlag(
                        countryCode = ipInfo.countryCode,
                        size = (20 * scaleFactor).dp
                    )
                } else {
                    Text(
                        text = "🌐",
                        fontSize = (16 * scaleFactor).sp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = when {
                            ipInfo.country.isNotEmpty() -> if (ipInfo.countryCode.isNotEmpty()) 
                                "${ipInfo.country} (${ipInfo.countryCode})" else ipInfo.country
                            ipInfo.isLoading -> "در حال دریافت..."
                            ipInfo.error != null -> "خطا"
                            else -> "ناشناخته"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = (11 * scaleFactor).sp
                    )
                    Text(
                        text = when {
                            ipInfo.ip.isNotEmpty() -> ipInfo.ip
                            ipInfo.isLoading -> "دریافت آی‌پی موقعیت شما..."
                            ipInfo.error != null -> "آی‌پی یافت نشد"
                            else -> "نمایش آی‌پی عمومی"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            ipInfo.error != null -> IosErrorRed
                            ipInfo.isLoading -> IosScanningAmber
                            else -> IosSecondaryLabel
                        },
                        fontSize = (9 * scaleFactor).sp
                    )
                }
            }

            if (ipInfo.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size((12 * scaleFactor).dp),
                    color = IosActiveBlue,
                    strokeWidth = 1.5.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "به‌روزرسانی",
                    tint = IosSecondaryLabel,
                    modifier = Modifier.size((12 * scaleFactor).dp)
                )
            }
        }
    }
}

// ==================== CONFIG CHIPS ROW ====================
@Composable
private fun ConfigChipsRow(
    config: AetherConfig,
    scaleFactor: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(IosGroupBg)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        IosConfigChip(
            label = "مسیر", 
            value = config.noise.displayName.split(" ")[0], 
            scaleFactor = scaleFactor
        )
        IosConfigChip(
            label = "سرعت", 
            value = config.scanMode.name.take(6), 
            scaleFactor = scaleFactor
        )
        IosConfigChip(
            label = "شبکه", 
            value = config.ipMode.rawValue, 
            scaleFactor = scaleFactor
        )
    }
}

// ==================== TRAFFIC VALUE ====================
@Composable
private fun TrafficValue(
    label: String, 
    value: String, 
    color: Color, 
    alignment: Alignment.Horizontal, 
    modifier: Modifier = Modifier, 
    speed: Double = 0.0, 
    scaleFactor: Float = 1f
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = IosSecondaryLabel,
            fontSize = (8 * scaleFactor).sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = (12 * scaleFactor).sp
        )
        if (speed > 0) {
            Text(
                text = formatSpeedValue(speed),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color.copy(alpha = 0.7f),
                fontSize = (8 * scaleFactor).sp
            )
        }
    }
}

// ==================== CONFIG CHIP ====================
@Composable
fun IosConfigChip(label: String, value: String, scaleFactor: Float = 1f) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = IosSecondaryLabel, 
            fontSize = (8 * scaleFactor).sp, 
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value, 
            style = MaterialTheme.typography.labelMedium, 
            fontWeight = FontWeight.Bold, 
            color = Color.White, 
            fontSize = (10 * scaleFactor).sp
        )
    }
}

// ==================== POWER BUTTON ====================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosPowerButton(
    connectionStatus: ConnectionStatus,
    onToggle: () -> Unit,
    onRecover: () -> Unit = {},
    size: Dp = 140.dp
) {
    val isConnected = connectionStatus == ConnectionStatus.RUNNING
    val isWorking = connectionStatus == ConnectionStatus.STARTING ||
                    connectionStatus == ConnectionStatus.VALIDATING ||
                    connectionStatus == ConnectionStatus.RECONNECTING ||
                    connectionStatus == ConnectionStatus.STOPPING
    val isError = connectionStatus == ConnectionStatus.ERROR

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "refinedGlow")

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isWorking) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing), 
            RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else if (isWorking) breathingScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, 
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    val cornerRadiusPercent by animateFloatAsState(
        targetValue = if (isConnected || isWorking) 0.28f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, 
            stiffness = Spring.StiffnessMedium
        ),
        label = "cornerRadius"
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            isConnected -> IosActiveGreen
            isWorking -> IosScanningAmber
            isError -> IosErrorRed
            else -> IosActiveBlue
        },
        animationSpec = tween(durationMillis = 600),
        label = "buttonColor"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = if (isConnected) 1.8f else 1.5f,
        animationSpec = infiniteRepeatable(
            tween(2500, easing = LinearEasing), 
            RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing), 
            RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .size(size * 2.5f),
        contentAlignment = Alignment.Center
    ) {
        if (isWorking || isConnected) {
            val pulseColor = buttonColor.copy(alpha = 0.45f)
            val glowShape = RoundedCornerShape(size * cornerRadiusPercent)

            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = glowScale
                        scaleY = glowScale
                        alpha = glowAlpha
                    }
                    .background(pulseColor, glowShape)
            )

            if (isConnected) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .graphicsLayer {
                            scaleX = glowScale * 0.75f
                            scaleY = glowScale * 0.75f
                            alpha = glowAlpha * 1.8f
                        }
                        .background(pulseColor, glowShape)
                )
            }
        }

        Surface(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                }
                .shadow(
                    elevation = if (isPressed) 6.dp else 24.dp,
                    shape = RoundedCornerShape(size * cornerRadiusPercent),
                    ambientColor = buttonColor.copy(alpha = 0.6f),
                    spotColor = buttonColor
                )
                .clip(RoundedCornerShape(size * cornerRadiusPercent))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = true,
                    onClick = {
                        scope.launch {
                            if (connectionStatus == ConnectionStatus.STOPPING) {
                                onRecover()
                            } else {
                                onToggle()
                            }
                        }
                    }
                ),
            color = buttonColor,
            tonalElevation = 14.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.45f)
                )
            }
        }
    }
}

// ==================== CAPSULE CONNECT BUTTON ====================
@Composable
fun CapsuleConnectButton(
    connectionStatus: ConnectionStatus,
    onToggle: () -> Boolean,
    modifier: Modifier = Modifier,
    scaleFactor: Float = 1f,
    onRecover: () -> Unit = {}
) {
    val sf = scaleFactor.coerceIn(0.7f, 1.1f)
    val isConnected = connectionStatus == ConnectionStatus.RUNNING
    val isWorking = connectionStatus in setOf(
        ConnectionStatus.STARTING, ConnectionStatus.VALIDATING, 
        ConnectionStatus.DATAPLANE_VALIDATED, ConnectionStatus.SOCKS_READY, 
        ConnectionStatus.TUN_ACTIVE, ConnectionStatus.RECONNECTING, 
        ConnectionStatus.STOPPING
    )
    val isError = connectionStatus == ConnectionStatus.ERROR
    
    val trackColor = when {
        isConnected -> IosActiveGreen
        isWorking -> IosScanningAmber
        isError -> IosErrorRed
        else -> IosGroupBg
    }
    
    val label = when {
        connectionStatus == ConnectionStatus.STOPPING -> "توقف اجباری"
        isWorking -> "در حال اتصال..."
        isConnected -> "قطع اتصال"
        isError -> "اتصال مجدد"
        else -> "اتصال"
    }
    
    Box(
        modifier = modifier
            .height((56 * sf).dp.coerceIn(48.dp, 64.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(trackColor)
            .clickable { 
                if (connectionStatus == ConnectionStatus.STOPPING) {
                    onRecover()
                } else {
                    onToggle()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isWorking) {
                CircularProgressIndicator(
                    modifier = Modifier.size((18 * sf).dp), 
                    color = Color.White, 
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width((8 * sf).dp))
            }
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = ((14 * sf).coerceIn(12f, 16f)).sp,
                letterSpacing = (0.6f * sf).sp
            )
        }
    }
}

// ==================== WINDOWS SWIPE SWITCH ====================
@Composable
fun WindowsSwipeSwitch(
    connectionStatus: ConnectionStatus,
    onToggle: () -> Boolean,
    modifier: Modifier = Modifier,
    scaleFactor: Float = 1f,
    onAdminCancelResetKey: Int = 0,
    onRecover: () -> Unit = {}
) {
    val isConnected = connectionStatus == ConnectionStatus.RUNNING
    val isWorking = connectionStatus in setOf(
        ConnectionStatus.STARTING, ConnectionStatus.VALIDATING,
        ConnectionStatus.DATAPLANE_VALIDATED, ConnectionStatus.SOCKS_READY,
        ConnectionStatus.TUN_ACTIVE, ConnectionStatus.RECONNECTING,
        ConnectionStatus.STOPPING
    )
    val isError = connectionStatus == ConnectionStatus.ERROR
    
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    val trackColor by animateColorAsState(
        targetValue = when {
            isConnected -> IosActiveGreen
            isWorking -> IosScanningAmber
            isError -> IosErrorRed
            else -> IosGroupBg
        }, 
        label = "trackColor"
    )
    
    val text = when (connectionStatus) {
        ConnectionStatus.STARTING -> "در یافتن سرورها..."
        ConnectionStatus.VALIDATING -> "در حال اعتبارسنجی..."
        ConnectionStatus.DATAPLANE_VALIDATED, ConnectionStatus.SOCKS_READY, 
        ConnectionStatus.TUN_ACTIVE -> "در حال اتصال..."
        ConnectionStatus.RECONNECTING -> "اتصال مجدد..."
        ConnectionStatus.STOPPING -> "برای توقف اجباری بکشید"
        ConnectionStatus.RUNNING -> "برای قطع اتصال بکشید"
        ConnectionStatus.ERROR, ConnectionStatus.FAILED -> "برای اتصال مجدد بکشید"
        ConnectionStatus.STOPPED -> "برای اتصال بکشید"
    }
    
    val hintTransition = rememberInfiniteTransition(label = "hint")
    val hintShift by hintTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing), 
            RepeatMode.Reverse
        ),
        label = "hintShift"
    )
    
    val dotTransition = rememberInfiniteTransition(label = "dots")
    val dotPhase by dotTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = LinearEasing), 
            RepeatMode.Restart
        ),
        label = "dotPhase"
    )
    
    val sf = scaleFactor.coerceIn(0.7f, 1.1f)
    val density = LocalDensity.current
    
    BoxWithConstraints(
        modifier = modifier
            .widthIn(min = (280 * sf).dp, max = (360 * sf).dp)
            .height((64 * sf).dp.coerceIn(52.dp, 72.dp))
            .shadow(12.dp, RoundedCornerShape(36.dp), spotColor = trackColor.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(36.dp))
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val thumbSize = (48 * sf).dp.coerceIn(42.dp, 56.dp)
        val thumbPx = with(density) { thumbSize.toPx() }
        val horizontalPadding = (8 * sf).dp
        val paddingPx = with(density) { horizontalPadding.toPx() }
        val maxDrag = (maxWidthPx - thumbPx - paddingPx * 2).coerceAtLeast(0f)
        val dragFraction = when {
            maxDrag == 0f -> 0f
            isConnected -> (1f - offsetX.value / maxDrag).coerceIn(0f, 1f)
            isWorking -> (offsetX.value / maxDrag).coerceIn(0f, 1f)
            else -> 0f
        }
        val isDisconnectDrag = (isConnected || isWorking) && isDragging && dragFraction > 0.05f
        val effectiveTrackColor = if (isDisconnectDrag) {
            lerp(trackColor, IosErrorRed, dragFraction)
        } else {
            trackColor
        }

        LaunchedEffect(isConnected, isWorking, maxDrag) {
            if (isDragging) return@LaunchedEffect
            if (isWorking) {
                offsetX.snapTo(if (isConnected) maxDrag else 0f)
            } else {
                offsetX.animateTo(
                    targetValue = if (isConnected) maxDrag else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy, 
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
        
        LaunchedEffect(onAdminCancelResetKey) {
            if (!isConnected && !isWorking && offsetX.value != 0f && !isDragging) {
                offsetX.animateTo(
                    0f, 
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(36.dp))
                .background(effectiveTrackColor.copy(alpha = if (isConnected || isDisconnectDrag) 1f else 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isDisconnectDrag) "برای قطع اتصال رها کنید" else text,
                color = Color.White.copy(alpha = 0.95f),
                fontWeight = FontWeight.Bold,
                fontSize = ((11 * sf).coerceIn(10f, 13f)).sp,
                letterSpacing = (0.6f * sf).sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = (56 * sf).dp)
            )
        }

        val hintOffset = if (!isDragging && !isWorking) {
            if (!isConnected) hintShift else -hintShift
        } else 0f
        
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = horizontalPadding)
                .offset { androidx.compose.ui.unit.IntOffset((offsetX.value + hintOffset).toInt(), 0) }
                .size(thumbSize)
                .shadow(8.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(isConnected, isWorking, maxDrag) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            scope.launch {
                                val threshold = if (isWorking) maxDrag * 0.25f else maxDrag * 0.5f
                                val shouldTrigger = if (isWorking) {
                                    if (!isConnected) offsetX.value > threshold 
                                    else offsetX.value < maxDrag - threshold
                                } else {
                                    if (!isConnected) offsetX.value > threshold 
                                    else offsetX.value < threshold
                                }
                                if (shouldTrigger) {
                                    val success = if (connectionStatus == ConnectionStatus.STOPPING) {
                                        onRecover()
                                        true
                                    } else onToggle()
                                    if (success) {
                                        if (isWorking) {
                                            offsetX.animateTo(
                                                0f, 
                                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                            )
                                        } else {
                                            offsetX.animateTo(
                                                if (!isConnected) maxDrag else 0f,
                                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                            )
                                        }
                                    } else {
                                        offsetX.animateTo(
                                            if (isConnected) maxDrag else 0f,
                                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                        )
                                    }
                                } else {
                                    offsetX.animateTo(
                                        if (isConnected) maxDrag else 0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch {
                                offsetX.animateTo(
                                    if (isConnected) maxDrag else 0f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val next = (offsetX.value + dragAmount).coerceIn(0f, maxDrag)
                                offsetX.snapTo(next)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isWorking && !isDragging) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = effectiveTrackColor,
                    strokeWidth = 2.5.dp
                )
            } else {
                Icon(
                    imageVector = if (isConnected || isWorking) {
                        Icons.AutoMirrored.Filled.ArrowBack
                    } else {
                        Icons.AutoMirrored.Filled.ArrowForward
                    },
                    contentDescription = null,
                    tint = IosActiveBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        if (!isWorking && !isConnected) {
            val connectFraction = if (maxDrag > 0f) (offsetX.value / maxDrag).coerceIn(0f, 1f) else 0f
            val rightAlpha = if (isDragging) (1f - connectFraction).coerceIn(0f, 1f) else 1f
            val rightShift = if (isDragging) connectFraction * 40f else hintShift * 0.6f
            
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .graphicsLayer { translationX = rightShift; alpha = rightAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { idx ->
                    val alpha = 0.3f + ((dotPhase + idx * 0.33f) % 1f) * 0.7f
                    Box(
                        modifier = Modifier
                            .padding(start = if (idx == 0) 0.dp else 3.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = alpha.coerceIn(0.3f, 1f)))
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(18.dp)
                        .padding(start = 4.dp)
                )
            }
        }
    }
}

// ==================== PICKER ROW ====================
@Composable
fun IosPickerRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    value: String,
    options: List<String>,
    onOptionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (options.isNotEmpty()) expanded = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            title,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            color = IosActiveBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            null,
            tint = IosSecondaryLabel,
            modifier = Modifier.size(14.dp)
        )
    }
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        options.forEachIndexed { index, option ->
            DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                    onOptionSelected(index)
                    expanded = false
                }
            )
        }
    }
}

// ==================== CONNECTION MODE SEGMENTED CONTROL ====================
@Composable
fun IosConnectionModeSegmentedControl(
    selectedMode: ConnectionMode,
    onModeSelected: (ConnectionMode) -> Unit,
    enabled: Boolean = true,
    scaleFactor: Float = 1f
) {
    val modes = listOf(
        ConnectionMode.TUNNEL to "حالت TUN",
        ConnectionMode.SYSTEM_PROXY to "پروکسی سیستم",
        ConnectionMode.PROXY_ONLY to "فقط پروکسی"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = IosCardBg,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IosCardBg)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modes.forEach { (mode, label) ->
                val selected = mode == selectedMode
                val bg by animateColorAsState(
                    targetValue = if (selected) IosActiveBlue else Color.Transparent,
                    animationSpec = tween(250), 
                    label = "modeBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) Color.White else IosSecondaryLabel,
                    animationSpec = tween(250), 
                    label = "modeText"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((36 * scaleFactor).dp)
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .shadow(
                            elevation = if (selected) 10.dp else 0.dp,
                            shape = RoundedCornerShape(50),
                            spotColor = IosActiveBlue.copy(alpha = 0.4f),
                            ambientColor = IosActiveBlue.copy(alpha = 0.3f)
                        )
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = enabled) { onModeSelected(mode) }
                        .graphicsLayer { alpha = if (enabled || selected) 1f else 0.45f }
                        .testTag("connection_mode_${mode.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.22f),
                                            Color.White.copy(alpha = 0.06f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = textColor,
                        fontSize = (10 * scaleFactor).sp,
                        letterSpacing = 0.3.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ==================== PROTOCOL SEGMENTED CONTROL ====================
@Composable
fun IosProtocolSegmentedControl(
    selectedProtocol: AetherProtocol,
    onProtocolSelected: (AetherProtocol) -> Unit,
    enabled: Boolean = true,
    allowedProtocols: Set<AetherProtocol>? = null,
    scaleFactor: Float = 1f
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = IosCardBg,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IosCardBg)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AetherProtocol.entries.forEach { proto ->
                val selected = proto == selectedProtocol
                val itemEnabled = enabled && (allowedProtocols == null || proto in allowedProtocols)
                val bg by animateColorAsState(
                    targetValue = if (selected) IosActiveBlue else Color.Transparent,
                    animationSpec = tween(250), 
                    label = "protoBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) Color.White else IosSecondaryLabel,
                    animationSpec = tween(250), 
                    label = "protoText"
                )
                val label = if (proto == AetherProtocol.ZERO_TRUST) {
                    "اعتماد صفر"
                } else {
                    proto.displayName.split(" ")[0].uppercase()
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((36 * scaleFactor).dp)
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .shadow(
                            elevation = if (selected) 10.dp else 0.dp,
                            shape = RoundedCornerShape(50),
                            spotColor = IosActiveBlue.copy(alpha = 0.4f),
                            ambientColor = IosActiveBlue.copy(alpha = 0.3f)
                        )
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = itemEnabled) { onProtocolSelected(proto) }
                        .graphicsLayer { alpha = if (itemEnabled || selected) 1f else 0.45f }
                        .testTag("protocol_${proto.rawValue}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.22f),
                                            Color.White.copy(alpha = 0.06f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = textColor,
                        fontSize = (10 * scaleFactor).sp,
                        letterSpacing = 0.3.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ==================== UTILITY FUNCTIONS ====================
private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    fun pad(n: Long) = if (n < 10) "0$n" else n.toString()
    return "${pad(h)}:${pad(m)}:${pad(s)}"
}

private fun formatTrafficBytes(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0)
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    var value = safeBytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    val roundedValue = (value * 100).toLong() / 100.0
    return if (unitIndex == 0) {
        "$safeBytes ${units[unitIndex]}"
    } else {
        "$roundedValue ${units[unitIndex]}"
    }
}

private fun formatSpeedValue(bytesPerSec: Double): String {
    return when {
        bytesPerSec >= 1024.0 * 1024.0 * 1024.0 * 1024.0 -> 
            "${"%.1f".format(bytesPerSec / (1024.0 * 1024.0 * 1024.0 * 1024.0))} TB/s"
        bytesPerSec >= 1024.0 * 1024.0 * 1024.0 -> 
            "${"%.1f".format(bytesPerSec / (1024.0 * 1024.0 * 1024.0))} GB/s"
        bytesPerSec >= 1024.0 * 1024.0 -> 
            "${"%.1f".format(bytesPerSec / (1024.0 * 1024.0))} MB/s"
        bytesPerSec >= 1024.0 -> 
            "${"%.0f".format(bytesPerSec / 1024.0)} KB/s"
        else -> 
            "${"%.0f".format(bytesPerSec)} B/s"
    }
}
