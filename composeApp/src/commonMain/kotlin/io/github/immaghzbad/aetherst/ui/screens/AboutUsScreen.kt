package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.immaghzbad.aetherst.platform.isDesktop
import io.github.immaghzbad.aetherst.shared.ui.components.IosActionRow
import io.github.immaghzbad.aetherst.shared.ui.components.SectionCard
import io.github.immaghzbad.aetherst.shared.ui.components.AppDivider
import io.github.immaghzbad.aetherst.shared.ui.theme.AppPalette
import io.github.immaghzbad.aetherst.shared.ui.theme.appColors

private val IosActiveBlue = AppPalette.accent
private val IosActiveGreen = AppPalette.statusConnected
private val IosPurple = AppPalette.accentVariant

private const val DeveloperTelegramUrl = "https://t.me/Feri_pm_tunnel"

@Composable
fun AboutUsScreen(
    appVersion: String = "1.0.0",
    bottomContentPadding: Dp = 0.dp
) {
    val uriHandler = LocalUriHandler.current
    val colors = appColors()
    Box(modifier = Modifier.fillMaxSize().background(colors.surfaceSunken)) {
        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + if (isDesktop) 16.dp else 12.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = topPadding,
                bottom = bottomContentPadding + 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { AboutHero(appVersion = appVersion) }
            item {
                SectionCard {
                    SectionTitle("معرفی")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Feri PM Tunnel یک کلاینت اتصال امن و خصوصی است که بر قابلیت اطمینان و سادگی تمرکز دارد — آمار لحظه‌ای، تنظیمات هوشمند و بدون شلوغی.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }
            item {
                SectionCard {
                    SectionTitle("چرا Feri PM Tunnel")
                    InfoRow(
                        title = "مقاوم در برابر سانسور",
                        description = "طراحی شده برای عبور از DPI و فیلترهای مبتنی بر پروتکل."
                    )
                    AppDivider()
                    InfoRow(
                        title = "ترابری ترکیبی",
                        description = "پشتیبانی از MASQUE (HTTP/2 و HTTP/3)، WireGuard و تونل‌های آبشاری."
                    )
                    AppDivider()
                    InfoRow(
                        title = "اعتبارسنجی دروازه",
                        description = "سلامت و یکپارچگی دروازه را قبل از مسیریابی داده‌ها تأیید می‌کند."
                    )
                    AppDivider()
                    InfoRow(
                        title = "بازیابی سریع",
                        description = "منطق اتصال مجدد خودکار که با تغییرات شبکه سازگار می‌شود."
                    )
                }
            }
            item {
                SectionCard {
                    SectionTitle("ارتباط با ما")
                    IosActionRow(
                        iconBg = IosActiveBlue.copy(alpha = 0.16f),
                        title = "تلگرام",
                        subtitle = "https://t.me/Feri_pm_tunnel",
                        onClick = { uriHandler.openUri(DeveloperTelegramUrl) }
                    )
                }
            }
            item { AboutFooter() }
        }
    }
}

@Composable
private fun AboutHero(appVersion: String) {
    val colors = appColors()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Feri PM Tunnel",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            fontSize = 30.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "کلاینت تونل‌زنی امن",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VersionChip(label = "برنامه", value = appVersion)
            VersionChip(label = "هسته", value = "1.7.0")
            VersionChip(label = "HEV", value = "2.17.1")
        }
    }
}

@Composable
private fun VersionChip(label: String, value: String) {
    val colors = appColors()
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.surfaceRaised,
        border = BorderStroke(0.5.dp, colors.divider)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InfoRow(title: String, description: String) {
    val colors = appColors()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val colors = appColors()
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = colors.textSecondary,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun AboutFooter() {
    val colors = appColors()
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "ساخته شده با ",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFD4AF37),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = " توسط Feri PM",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = " همیشه متصل باشید هیچ نگران حجم نباشید به فرشاد پی ام بسپارید.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
