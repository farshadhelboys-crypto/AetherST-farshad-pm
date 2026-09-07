package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    platformContext?.let { getSettings(it) }

    val isRunning = connectionStatus == ConnectionStatus.RUNNING ||
        connectionStatus == ConnectionStatus.TUN_ACTIVE ||
        connectionStatus == ConnectionStatus.SOCKS_READY
    val isBusy = connectionStatus == ConnectionStatus.STARTING ||
        connectionStatus == ConnectionStatus.VALIDATING ||
        connectionStatus == ConnectionStatus.RECONNECTING ||
        connectionStatus == ConnectionStatus.STOPPING ||
        connectionStatus == ConnectionStatus.DATAPLANE_VALIDATED ||
        isFariKnightActive

    val pulse by rememberInfiniteTransition(label = "connectPulse").animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning || isBusy) 1.08f else 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )

    val brandShift by rememberInfiniteTransition(label = "brand").animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "brandShift"
    )
    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    val statusText = when {
        isFariKnightActive && !isRunning -> "فری شوالیه در حال جستجوی بهترین مسیر..."
        isRunning -> "اتصال امن برقرار است"
        isBusy -> "در حال برقراری اتصال..."
        connectionStatus == ConnectionStatus.ERROR || connectionStatus == ConnectionStatus.FAILED -> "اتصال ناموفق بود"
        else -> "آماده اتصال"
    }

    val statusColor = when {
        isFariKnightActive && !isRunning -> Color(0xFFFFB300)
        isRunning -> AppPalette.statusConnected
        connectionStatus == ConnectionStatus.ERROR || connectionStatus == ConnectionStatus.FAILED -> AppPalette.statusError
        isBusy -> AppPalette.statusScanning
        else -> AppPalette.accent
    }

    fun toggle() {
        if (connectionStatus == ConnectionStatus.STOPPING) onForceStop() else onToggleVpn()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF090A0F), Color(0xFF10131B), Color(0xFF08090D))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = bottomContentPadding + 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Feri Pm Tunnel", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    Text("تونل سریع، امن و خصوصی", color = AppPalette.textSecondary, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .clickable { showInfo = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
                }
            }

            // Connection hero
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(statusColor.copy(alpha = 0.24f), Color(0xFF141720), Color(0xFF11131A))
                            )
                        )
                        .padding(vertical = 22.dp, horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(18.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .scale(pulse)
                                    .border(1.dp, statusColor.copy(alpha = 0.25f), CircleShape)
                                    .padding(13.dp)
                                    .border(1.dp, statusColor.copy(alpha = 0.55f), CircleShape)
                                    .padding(18.dp)
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.13f))
                            )
                            IconButton(
                                onClick = { toggle() },
                                enabled = !isBusy || isRunning,
                                modifier = Modifier.size(112.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(statusColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isBusy && !isRunning) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.PowerSettingsNew,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // farshad pm special effect
                        Spacer(modifier = Modifier.height(8.dp))
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

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            if (isRunning) formatDuration(elapsedSeconds) else "برای شروع، دکمه اتصال را بزنید",
                            color = Color.White,
                            fontSize = if (isRunning) 22.sp else 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // فری شوالیه
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
                                .fillMaxWidth(0.88f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
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
                            modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp)
                        )
                    }
                }
            }

            // Quick stats: total + speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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

            if (!isDesktop) {
                PlatformSubscriptionCard()
            }

            // Network information
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.055f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppPalette.accent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (ipInfo.flagEmoji.isNotBlank()) {
                                    Text(ipInfo.flagEmoji, fontSize = 18.sp)
                                } else {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null,
                                        tint = AppPalette.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("شبکه فعلی", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    when {
                                        ipInfo.isLoading -> "در حال دریافت موقعیت..."
                                        ipInfo.country.isNotBlank() -> ipInfo.country
                                        else -> "اطلاعات شبکه"
                                    },
                                    color = AppPalette.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        IconButton(onClick = onRefreshIpInfo) {
                            if (ipInfo.isLoading) {
                                CircularProgressIndicator(
                                    color = AppPalette.accent,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = AppPalette.textSecondary)
                            }
                        }
                    }
                    InfoRow(
                        title = "IP",
                        value = if (ipInfo.ip.isNotBlank()) ipInfo.ip else "—",
                        onCopy = { if (ipInfo.ip.isNotBlank()) onCopy(ipInfo.ip) }
                    )
                    InfoRow(
                        title = "کشور",
                        value = when {
                            ipInfo.country.isNotBlank() && ipInfo.countryCode.isNotBlank() ->
                                "${ipInfo.country} (${ipInfo.countryCode})"
                            ipInfo.country.isNotBlank() -> ipInfo.country
                            ipInfo.error != null -> ipInfo.error ?: "خطا"
                            else -> "—"
                        },
                        onCopy = null
                    )
                    InfoRow(
                        title = "پروتکل",
                        value = config.protocol.displayName,
                        onCopy = null
                    )
                }
            }

            // Protection strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.045f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = statusColor, modifier = Modifier.size(21.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("محافظت اتصال", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        if (isRunning) "ترافیک از تونل عبور می‌کند" else "پس از اتصال فعال می‌شود",
                        color = AppPalette.textSecondary,
                        fontSize = 10.sp
                    )
                }
                Text(
                    if (isRunning) "فعال" else "آماده",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Text(
                "نسخه $appVersion",
                color = AppPalette.textSecondary,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showInfo) {
        Dialog(
            onDismissRequest = { showInfo = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171A22))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AppPalette.accent, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Feri Pm Tunnel", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("farshad pm", color = AppPalette.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("پنل جدید و ساده‌شده", color = AppPalette.textSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { showInfo = false },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.055f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = AppPalette.accent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    color = AppPalette.accent.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
            Text(title, color = AppPalette.textSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun InfoRow(
    title: String,
    value: String,
    onCopy: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = AppPalette.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier.width(60.dp)
        )
        Text(
            value,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        if (onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = AppPalette.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
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
