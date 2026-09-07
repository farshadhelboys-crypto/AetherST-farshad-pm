package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.getSettings
import io.github.immaghzbad.aetherst.platform.isDesktop
import io.github.immaghzbad.aetherst.shared.data.IpInfo
import io.github.immaghzbad.aetherst.shared.data.PingState
import io.github.immaghzbad.aetherst.shared.model.AetherConfig
import io.github.immaghzbad.aetherst.shared.model.AetherProtocol
import io.github.immaghzbad.aetherst.shared.model.ConnectionStatus
import io.github.immaghzbad.aetherst.shared.model.SessionTraffic
import io.github.immaghzbad.aetherst.subscription.PlatformSubscriptionCard
import io.github.immaghzbad.aetherst.shared.ui.theme.AppPalette

@Composable
fun DashboardScreenV2(
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
    onFariKnight: () -> Unit = {},
    isFariKnightActive: Boolean = false,
    bottomContentPadding: Dp = 0.dp,
    platformContext: PlatformContext? = null
) {
    var showInfo by remember { mutableStateOf(false) }
    val settings = platformContext?.let { getSettings(it) }

    val isRunning = connectionStatus == ConnectionStatus.RUNNING ||
        connectionStatus == ConnectionStatus.TUN_ACTIVE ||
        connectionStatus == ConnectionStatus.SOCKS_READY
    val isBusy = connectionStatus == ConnectionStatus.STARTING ||
        connectionStatus == ConnectionStatus.VALIDATING ||
        connectionStatus == ConnectionStatus.RECONNECTING ||
        connectionStatus == ConnectionStatus.STOPPING ||
        connectionStatus == ConnectionStatus.DATAPLANE_VALIDATED ||
        isFariKnightActive

    val statusText = when {
        isFariKnightActive && !isRunning -> "فری شوالیه در حال جستجوی بهترین مسیر..."
        connectionStatus == ConnectionStatus.ERROR || connectionStatus == ConnectionStatus.FAILED -> "اتصال ناموفق بود"
        isBusy && !isRunning -> "در حال برقراری ارتباط..."
        isRunning -> "متصل"
        else -> "قطع"
    }
    val statusColor = when {
        isFariKnightActive && !isRunning -> Color(0xFFFFB300)
        connectionStatus == ConnectionStatus.ERROR || connectionStatus == ConnectionStatus.FAILED -> AppPalette.statusError
        isBusy && !isRunning -> AppPalette.statusScanning
        isRunning -> AppPalette.statusConnected
        else -> AppPalette.textSecondary
    }

    fun toggle() {
        if (connectionStatus == ConnectionStatus.STOPPING) onForceStop() else onToggleVpn()
    }

    val infinite = rememberInfiniteTransition(label = "dash")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val brandShift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "brand"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1220), Color(0xFF111827), Color(0xFF0B1220))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Feri Pm Tunnel", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("نسخه $appVersion", color = AppPalette.textSecondary, fontSize = 11.sp)
            }
            Row {
                IconButton(onClick = { showInfo = true }) {
                    Icon(Icons.Default.Lock, null, tint = AppPalette.textSecondary)
                }
                IconButton(onClick = onOpenSettingsToZeroTrust) {
                    Icon(Icons.Default.Settings, null, tint = AppPalette.textSecondary)
                }
            }
        }

        // Connect card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size((130 * if (isRunning || isBusy) pulse else 1f).dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.25f), CircleShape)
                            .padding(13.dp)
                            .border(1.dp, statusColor.copy(alpha = 0.55f), CircleShape)
                            .padding(18.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.13f))
                    )
                    IconButton(onClick = { toggle() }, enabled = !isBusy || isRunning, modifier = Modifier.size(112.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(statusColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBusy && !isRunning) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            } else {
                                Icon(Icons.Default.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(40.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Special effect brand text: farshad pm
                val brandBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF60A5FA),
                        Color(0xFFA78BFA),
                        Color(0xFFF472B6),
                        Color(0xFF60A5FA)
                    ),
                    start = Offset(brandShift, 0f),
                    end = Offset(brandShift + 200f, 80f)
                )
                Text(
                    text = "farshad pm",
                    style = TextStyle(
                        brush = brandBrush,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(
                            color = Color(0xFF8B5CF6).copy(alpha = glowAlpha),
                            offset = Offset(0f, 0f),
                            blurRadius = 18f
                        )
                    ),
                    modifier = Modifier.alpha(0.95f)
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    if (isRunning) formatDuration(elapsedSeconds) else "برای شروع، دکمه اتصال را بزنید",
                    color = Color.White,
                    fontSize = if (isRunning) 22.sp else 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.height(14.dp))

                // فری شوالیه button
                Button(
                    onClick = onFariKnight,
                    enabled = !isRunning || isFariKnightActive,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFariKnightActive) Color(0xFFFFB300) else Color(0xFF7C3AED),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF7C3AED).copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isFariKnightActive) "توقف فری شوالیه" else "فری شوالیه",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    "اگر وصل نشد، همه پروتکل‌ها را مرحله‌به‌مرحله امتحان می‌کند",
                    color = AppPalette.textSecondary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, start = 16.dp, end = 16.dp)
                )
            }
        }

        // Traffic stats: total + speed
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "دانلود",
                value = formatTrafficBytes(sessionTraffic.downloadedBytes),
                subtitle = formatRate(sessionTraffic.downloadSpeedBps),
                icon = Icons.Default.Speed,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "آپلود",
                value = formatTrafficBytes(sessionTraffic.uploadedBytes),
                subtitle = formatRate(sessionTraffic.uploadSpeedBps),
                icon = Icons.Default.Language,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "پینگ",
                value = if (pingState.ms >= 0) "${pingState.ms} ms" else "—",
                subtitle = "تأخیر",
                icon = Icons.Default.Speed,
                modifier = Modifier.weight(1f)
            )
        }

        // Subscription
        if (!isDesktop) {
            PlatformSubscriptionCard()
        }

        // Network information (country / IP)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            Column(Modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (ipInfo.flagEmoji.isNotBlank()) {
                            Text(ipInfo.flagEmoji, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Language, null, tint = AppPalette.accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Column {
                            Text(
                                when {
                                    ipInfo.isLoading -> "در حال دریافت موقعیت..."
                                    ipInfo.country.isNotBlank() -> ipInfo.country
                                    else -> "اطلاعات شبکه"
                                },
                                color = AppPalette.textSecondary,
                                fontSize = 11.sp
                            )
                            if (ipInfo.countryCode.isNotBlank()) {
                                Text(ipInfo.countryCode, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            }
                        }
                    }
                    IconButton(onClick = onRefreshIpInfo) {
                        if (ipInfo.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AppPalette.accent)
                        } else {
                            Icon(Icons.Default.Refresh, null, tint = AppPalette.textSecondary)
                        }
                    }
                }
                InfoRow("IP", if (ipInfo.ip.isNotBlank()) ipInfo.ip else "—", onCopy = { if (ipInfo.ip.isNotBlank()) onCopy(ipInfo.ip) })
                InfoRow("کشور", when {
                    ipInfo.country.isNotBlank() && ipInfo.countryCode.isNotBlank() -> "${ipInfo.country} (${ipInfo.countryCode})"
                    ipInfo.country.isNotBlank() -> ipInfo.country
                    ipInfo.error != null -> ipInfo.error ?: "خطا"
                    else -> "—"
                }, onCopy = null)
                InfoRow("پروتکل", config.protocol.displayName, onCopy = null)
            }
        }

        // Protocol chips
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            Column(Modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("پروتکل فعال", color = AppPalette.textSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    val protocols = AetherProtocol.entries.filter { it != AetherProtocol.ZERO_TRUST }
                    protocols.forEach { proto ->
                        val selected = config.protocol == proto
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) AppPalette.accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                                .border(
                                    width = if (selected) 1.dp else 0.dp,
                                    color = if (selected) AppPalette.accent else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = !isBusy) { onUpdateProtocol(proto) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                proto.displayName,
                                color = if (selected) Color.White else AppPalette.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInfo) {
        Dialog(onDismissRequest = { showInfo = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, null, tint = AppPalette.accent, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Feri Pm Tunnel", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("farshad pm", color = AppPalette.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("پنل جدید و ساده‌شده", color = AppPalette.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = { showInfo = false }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("بستن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.055f))
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = AppPalette.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(7.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = AppPalette.accent.copy(alpha = 0.9f), fontSize = 10.sp, textAlign = TextAlign.Center)
            }
            Text(title, color = AppPalette.textSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun InfoRow(title: String, value: String, onCopy: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = AppPalette.textSecondary, fontSize = 11.sp, modifier = Modifier.width(60.dp))
        Text(value, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
        if (onCopy != null) IconButton(onClick = onCopy, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.ContentCopy, null, tint = AppPalette.textSecondary, modifier = Modifier.size(15.dp))
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun formatRate(bps: Double): String {
    if (bps <= 0.0) return "0 KB/s"
    val kb = bps / 1024.0
    return if (kb >= 1024.0) "%.1f MB/s".format(kb / 1024.0) else "%.0f KB/s".format(kb)
}

private fun formatTrafficBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.2f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.0f KB".format(kb)
        else -> "$bytes B"
    }
}
